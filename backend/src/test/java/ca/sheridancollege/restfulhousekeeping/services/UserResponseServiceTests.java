package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.EntityManager;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.Organization;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.OrganizationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserResponseServiceTests {

    @Autowired MockMvc mvc;
    @Autowired UserResponseService service;
    @Autowired CleaningRepository cleaningRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;

    Organization organization;
    User manager;
    User cleaner;
    Property property;

    @BeforeEach
    void setup() {
        organization = organizationRepository.save(
                Organization.builder().name("Team Removal Test").build());
        manager = userRepository.save(User.builder()
                .username("team-removal-manager")
                .email("team-removal-manager@example.com")
                .role(Role.MANAGER)
                .organization(organization)
                .build());
        cleaner = userRepository.save(User.builder()
                .username("team-removal-cleaner")
                .email("team-removal-cleaner@example.com")
                .role(Role.CLEANER)
                .organization(organization)
                .build());
        property = propertyRepository.save(Property.builder()
                .name("Team Removal Property")
                .manager(manager)
                .build());
    }

    @Test
    void removesCleanerFromTeamWithoutDeletingAccountAndUnassignsOnlyUpcomingCleanings() {
        Cleaning upcoming = saveCleaning(LocalDateTime.now().plusDays(2));
        Cleaning past = saveCleaning(LocalDateTime.now().minusDays(2));

        service.removeCleanerFromTeam(cleaner.getId(), manager);
        entityManager.flush();
        entityManager.clear();

        User savedCleaner = userRepository.findById(cleaner.getId()).orElseThrow();
        assertThat(savedCleaner.getOrganization()).isNull();
        assertThat(userRepository.findByUsernameIgnoreCase(cleaner.getUsername()))
                .contains(savedCleaner);
        assertThat(cleaningRepository.findById(upcoming.getId()).orElseThrow()
                .getCleaner()).isNull();
        assertThat(cleaningRepository.findById(past.getId()).orElseThrow()
                .getCleaner().getId()).isEqualTo(savedCleaner.getId());
    }

    @Test
    void rejectsCleanerFromAnotherManagersTeam() {
        Organization otherOrganization = organizationRepository.save(
                Organization.builder().name("Other Team").build());
        User otherManager = userRepository.save(User.builder()
                .username("other-team-removal-manager")
                .email("other-team-removal-manager@example.com")
                .role(Role.MANAGER)
                .organization(otherOrganization)
                .build());
        Cleaning upcoming = saveCleaning(LocalDateTime.now().plusDays(2));

        assertThatThrownBy(() -> service.removeCleanerFromTeam(
                cleaner.getId(), otherManager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN));

        assertThat(userRepository.findById(cleaner.getId()).orElseThrow()
                .getOrganization()).isEqualTo(organization);
        assertThat(cleaningRepository.findById(upcoming.getId()).orElseThrow()
                .getCleaner()).isEqualTo(cleaner);
    }

    @Test
    void managerCanUseRemoveTeamMemberEndpoint() throws Exception {
        Cleaning upcoming = saveCleaning(LocalDateTime.now().plusDays(2));
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        manager, null, manager.getAuthorities());

        mvc.perform(delete("/cleaners/{cleanerId}", cleaner.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.existsById(cleaner.getId())).isTrue();
        assertThat(userRepository.findById(cleaner.getId()).orElseThrow()
                .getOrganization()).isNull();
        assertThat(cleaningRepository.findById(upcoming.getId()).orElseThrow()
                .getCleaner()).isNull();
    }

    @Test
    void cleanerCannotUseRemoveTeamMemberEndpoint() throws Exception {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        cleaner, null, cleaner.getAuthorities());

        mvc.perform(delete("/cleaners/{cleanerId}", cleaner.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isForbidden());

        assertThat(userRepository.findById(cleaner.getId()).orElseThrow()
                .getOrganization()).isEqualTo(organization);
    }

    private Cleaning saveCleaning(LocalDateTime start) {
        return cleaningRepository.save(Cleaning.builder()
                .manager(manager)
                .cleaner(cleaner)
                .property(property)
                .dateTimeStart(start)
                .dateTimeEnd(start.plusHours(2))
                .isComplete(false)
                .build());
    }
}
