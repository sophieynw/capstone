package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
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

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CleaningChecklistItemServiceTests {

    @Autowired MockMvc mvc;
    @Autowired CleaningChecklistItemService service;
    @Autowired CleaningResponseService cleaningResponseService;
    @Autowired CleaningChecklistItemRepository itemRepository;
    @Autowired CleaningRepository cleaningRepository;
    @Autowired PropertyRepository propertyRepository;
    @Autowired UserRepository userRepository;

    User manager;
    User cleaner;
    Cleaning cleaning;

    @BeforeEach
    void setup() {
        manager = userRepository.save(User.builder()
                .username("checklist-manager")
                .email("checklist-manager@example.com")
                .role(Role.MANAGER)
                .build());
        cleaner = userRepository.save(User.builder()
                .username("checklist-cleaner")
                .email("checklist-cleaner@example.com")
                .role(Role.CLEANER)
                .build());
        Property property = propertyRepository.save(Property.builder()
                .name("Checklist Test Property")
                .manager(manager)
                .build());
        cleaning = cleaningRepository.save(Cleaning.builder()
                .manager(manager)
                .cleaner(cleaner)
                .property(property)
                .dateTimeStart(LocalDateTime.now().plusDays(1))
                .isComplete(false)
                .build());
    }

    @Test
    void managerCanAddCustomItemThroughEndpoint() throws Exception {
        mvc.perform(post("/cleanings/{cleaningId}/checklist-items", cleaning.getId())
                        .with(authentication(authenticationFor(manager)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"  Wipe baseboards  \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").value("Wipe baseboards"))
                .andExpect(jsonPath("$.isComplete").value(false))
                .andExpect(jsonPath("$.frequencyDays").doesNotExist())
                .andExpect(jsonPath("$.lastCompleted").doesNotExist());

        assertThat(itemRepository.findAllByCleaning_Id(cleaning.getId()))
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getCustomDescription()).isEqualTo("Wipe baseboards");
                    assertThat(item.getIsComplete()).isFalse();
                });
    }

    @Test
    void managerCanDeleteItemThroughEndpoint() throws Exception {
        CleaningChecklistItem item = saveCustomItem(cleaning, "Clean mirrors");

        mvc.perform(delete(
                        "/cleanings/{cleaningId}/checklist-items/{itemId}",
                        cleaning.getId(),
                        item.getId()
                ).with(authentication(authenticationFor(manager))))
                .andExpect(status().isNoContent());

        assertThat(itemRepository.existsById(item.getId())).isFalse();
    }

    @Test
    void doesNotDeleteItemFromAnotherCleaning() {
        Cleaning otherCleaning = cleaningRepository.save(Cleaning.builder()
                .manager(manager)
                .property(cleaning.getProperty())
                .isComplete(false)
                .build());
        CleaningChecklistItem item = saveCustomItem(otherCleaning, "Other item");

        assertThatThrownBy(() -> service.deleteItem(
                cleaning.getId(), item.getId(), manager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.NOT_FOUND));

        assertThat(itemRepository.existsById(item.getId())).isTrue();
    }

    @Test
    void rejectsManagerWhoDoesNotOwnCleaning() {
        User otherManager = userRepository.save(User.builder()
                .username("other-checklist-manager")
                .email("other-checklist-manager@example.com")
                .role(Role.MANAGER)
                .build());

        assertThatThrownBy(() -> service.addCustomItem(
                cleaning.getId(), "Unauthorized item", otherManager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.FORBIDDEN));
    }

    @Test
    void rejectsChangesToCompletedCleaningChecklist() {
        CleaningChecklistItem item = saveCustomItem(cleaning, "Existing item");
        cleaning.setIsComplete(true);
        cleaningRepository.save(cleaning);

        assertThatThrownBy(() -> service.addCustomItem(
                cleaning.getId(), "New item", manager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT));

        assertThatThrownBy(() -> service.deleteItem(
                cleaning.getId(), item.getId(), manager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.CONFLICT));

        assertThat(itemRepository.existsById(item.getId())).isTrue();
    }

    @Test
    void cleanerRoleCannotAccessChecklistEndpoints() throws Exception {
        mvc.perform(post("/cleanings/{cleaningId}/checklist-items", cleaning.getId())
                        .with(authentication(authenticationFor(cleaner)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Unauthorized item\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void newlyAddedItemIsIncludedWhenCleanerFetchesUpcomingCleaning() {
        service.addCustomItem(cleaning.getId(), "Clean coffee maker", manager);

        List<CleaningResponse> cleanings =
                cleaningResponseService.getMyUpcomingCleanings(cleaner.getId());

        assertThat(cleanings).singleElement();

        assertThat(cleanings.getFirst().getCleaningChecklistItems())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.getDescription()).isEqualTo("Clean coffee maker");
                    assertThat(item.getIsComplete()).isFalse();
                });
    }

    private CleaningChecklistItem saveCustomItem(
            Cleaning targetCleaning,
            String description) {
        return itemRepository.save(CleaningChecklistItem.builder()
                .cleaning(targetCleaning)
                .customDescription(description)
                .isComplete(false)
                .build());
    }

    private UsernamePasswordAuthenticationToken authenticationFor(User user) {
        return new UsernamePasswordAuthenticationToken(
                user,
                null,
                user.getAuthorities()
        );
    }
}
