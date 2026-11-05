package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.persistence.EntityManager;
import java.time.LocalDateTime;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Organization;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.OrganizationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CleaningResponseServiceTests {

    @Autowired MockMvc mvc;
    @Autowired CleaningResponseService service;
    @Autowired CleaningRepository cleaningRepository;
    @Autowired CleaningChecklistItemRepository checklistItemRepository;
    @Autowired OrganizationRepository organizationRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired UserRepository userRepository;
    @Autowired EntityManager entityManager;

    Organization managerOrganization;
    User manager;
    User teamCleaner;
    Cleaning cleaning;

    @BeforeEach
    void setup() {
        managerOrganization = organizationRepository.save(
                Organization.builder().name("Assignment Test Team").build()
        );
        manager = userRepository.save(User.builder()
                .username("assignment-manager")
                .email("assignment-manager@example.com")
                .role(Role.MANAGER)
                .organization(managerOrganization)
                .build());
        teamCleaner = userRepository.save(User.builder()
                .username("assignment-cleaner")
                .email("assignment-cleaner@example.com")
                .role(Role.CLEANER)
                .organization(managerOrganization)
                .build());
        Property property = propertyRepository.save(Property.builder()
                .name("Assignment Test Property")
                .manager(manager)
                .build());
        cleaning = cleaningRepository.save(Cleaning.builder()
                .manager(manager)
                .property(property)
                .isComplete(false)
                .build());
    }

    @Test
    void completesAndPersistsOnlyTheSelectedCleaningsChecklist() throws Exception {
        cleaning.setCleaner(teamCleaner);
        Cleaning otherCleaning = cleaningRepository.save(Cleaning.builder()
                .manager(manager)
                .property(cleaning.getProperty())
                .isComplete(false)
                .build());
        checklistItemRepository.save(CleaningChecklistItem.builder()
                .cleaning(cleaning)
                .customDescription("Clean the kitchen")
                .build());
        checklistItemRepository.save(CleaningChecklistItem.builder()
                .cleaning(cleaning)
                .customDescription("Change the bedding")
                .isComplete(true)
                .build());
        CleaningChecklistItem otherItem = checklistItemRepository.save(
                CleaningChecklistItem.builder()
                        .cleaning(otherCleaning)
                        .customDescription("Clean the bathroom")
                        .build());
        entityManager.flush();
        entityManager.clear();

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        teamCleaner, null, teamCleaner.getAuthorities());
        mvc.perform(patch("/cleanings/{cleaningId}/complete", cleaning.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isComplete").value(true))
                .andExpect(jsonPath("$.cleaningChecklistItems[*].isComplete")
                        .value(org.hamcrest.Matchers.contains(true, true)));

        entityManager.flush();
        entityManager.clear();
        assertThat(cleaningRepository.findById(cleaning.getId()).orElseThrow()
                .getIsComplete()).isTrue();
        assertThat(checklistItemRepository.findAllByCleaning_Id(cleaning.getId()))
                .extracting(CleaningChecklistItem::getIsComplete)
                .containsExactly(true, true);
        assertThat(checklistItemRepository.findById(otherItem.getId()).orElseThrow()
                .getIsComplete()).isFalse();
    }

    @Test
    void assignsCleanerFromManagersTeam() {
        CleaningResponse response = service.updateCleaner(
                cleaning.getId(), teamCleaner.getId(), manager);

        assertThat(response.getCleanerId()).isEqualTo(teamCleaner.getId());
        assertThat(cleaningRepository.findById(cleaning.getId()).orElseThrow()
                .getCleaner().getId()).isEqualTo(teamCleaner.getId());
    }

    @Test
    void allowsManagerToUnassignCleaner() {
        cleaning.setCleaner(teamCleaner);
        cleaningRepository.save(cleaning);

        CleaningResponse response = service.updateCleaner(
                cleaning.getId(), null, manager);

        assertThat(response.getCleanerId()).isNull();
        assertThat(cleaningRepository.findById(cleaning.getId()).orElseThrow()
                .getCleaner()).isNull();
    }

    @Test
    void rejectsCleaningManagedBySomeoneElse() {
        User otherManager = userRepository.save(User.builder()
                .username("other-assignment-manager")
                .email("other-assignment-manager@example.com")
                .role(Role.MANAGER)
                .organization(managerOrganization)
                .build());
        cleaning.setManager(otherManager);
        cleaningRepository.save(cleaning);

        assertForbidden(() -> service.updateCleaner(
                cleaning.getId(), teamCleaner.getId(), manager));

        assertThat(cleaning.getCleaner()).isNull();
    }

    @Test
    void rejectsCleanerFromAnotherTeam() {
        Organization otherOrganization = organizationRepository.save(
                Organization.builder().name("Other Assignment Team").build()
        );
        User outsider = userRepository.save(User.builder()
                .username("outsider-assignment-cleaner")
                .email("outsider-assignment-cleaner@example.com")
                .role(Role.CLEANER)
                .organization(otherOrganization)
                .build());

        assertForbidden(() -> service.updateCleaner(
                cleaning.getId(), outsider.getId(), manager));

        assertThat(cleaning.getCleaner()).isNull();
    }

    @Test
    void rejectsNonManagerEvenIfTheirIdMatches() {
        User cleanerPrincipal = User.builder()
                .id(manager.getId())
                .role(Role.CLEANER)
                .build();

        assertForbidden(() -> service.updateCleaner(
                cleaning.getId(), teamCleaner.getId(), cleanerPrincipal));

        assertThat(cleaning.getCleaner()).isNull();
    }

    @Test
    void rejectsReassignmentAfterCleaningIsComplete() {
        cleaning.setIsComplete(true);
        cleaningRepository.save(cleaning);

        assertThatThrownBy(() -> service.updateCleaner(
                cleaning.getId(), teamCleaner.getId(), manager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT));

        assertThat(cleaning.getCleaner()).isNull();
    }

    @Test
    void managerCanUpdateCleaningSchedule() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 2, 10, 30);
        LocalDateTime end = LocalDateTime.of(2026, 11, 2, 13, 0);

        CleaningResponse response = service.updateSchedule(
                cleaning.getId(), start, end, manager);

        assertThat(response.getDateTimeStart()).isEqualTo(start);
        assertThat(response.getDateTimeEnd()).isEqualTo(end);
        Cleaning saved = cleaningRepository.findById(cleaning.getId()).orElseThrow();
        assertThat(saved.getDateTimeStart()).isEqualTo(start);
        assertThat(saved.getDateTimeEnd()).isEqualTo(end);
    }

    @Test
    void rejectsScheduleUpdateAfterCleaningIsComplete() {
        cleaning.setIsComplete(true);
        cleaningRepository.save(cleaning);

        LocalDateTime start = LocalDateTime.of(2026, 11, 2, 10, 30);
        LocalDateTime end = LocalDateTime.of(2026, 11, 2, 13, 0);

        assertThatThrownBy(() -> service.updateSchedule(
                cleaning.getId(), start, end, manager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void rejectsScheduleWhoseEndIsNotAfterStart() {
        LocalDateTime start = LocalDateTime.of(2026, 11, 2, 13, 0);
        LocalDateTime end = LocalDateTime.of(2026, 11, 2, 10, 30);

        assertThatThrownBy(() -> service.updateSchedule(
                cleaning.getId(), start, end, manager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void cleanerRoleCannotAccessAssignmentEndpoint() throws Exception {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        teamCleaner,
                        null,
                        teamCleaner.getAuthorities()
                );

        mvc.perform(patch("/cleanings/{cleaningId}/cleaner", cleaning.getId())
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"cleanerId\":null}"))
                .andExpect(status().isForbidden());

        assertThat(cleaning.getCleaner()).isNull();
    }

    @Test
    void cleanerRoleCannotAccessScheduleEndpoint() throws Exception {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        teamCleaner,
                        null,
                        teamCleaner.getAuthorities()
                );

        mvc.perform(patch("/cleanings/{cleaningId}/schedule", cleaning.getId())
                        .with(authentication(authentication))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "dateTimeStart": "2026-11-02T10:30:00",
                                  "dateTimeEnd": "2026-11-02T13:00:00"
                                }
                                """))
                .andExpect(status().isForbidden());

        Cleaning saved = cleaningRepository.findById(cleaning.getId()).orElseThrow();
        assertThat(saved.getDateTimeStart()).isNull();
        assertThat(saved.getDateTimeEnd()).isNull();
    }

    @Test
    void managerCanDeleteIncompleteCleaning() throws Exception {
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        manager,
                        null,
                        manager.getAuthorities()
                );

        mvc.perform(delete("/cleanings/{cleaningId}", cleaning.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isNoContent());

        assertThat(cleaningRepository.existsById(cleaning.getId())).isFalse();
    }

    @Test
    void managerCannotDeleteCompletedCleaning() throws Exception {
        cleaning.setIsComplete(true);
        cleaningRepository.save(cleaning);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        manager,
                        null,
                        manager.getAuthorities()
                );

        mvc.perform(delete("/cleanings/{cleaningId}", cleaning.getId())
                        .with(authentication(authentication)))
                .andExpect(status().isConflict());

        assertThat(cleaningRepository.existsById(cleaning.getId())).isTrue();
    }

    private void assertForbidden(Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
