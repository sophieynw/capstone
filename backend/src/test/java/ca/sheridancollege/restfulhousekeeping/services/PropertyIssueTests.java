package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PropertyIssueTests {

    @Autowired MockMvc mvc;
    @Autowired EntityManager entityManager;
    @Autowired CleaningRepository cleanings;
    @Autowired PropertyRepository properties;
    @Autowired UserRepository users;

    @Test
    void issueSurvivesCleaningDeletionAndBelongsToThePropertyUntilItIsDeleted() throws Exception {
        User manager = saveUser("issue-manager", Role.MANAGER);
        User cleaner = saveUser("issue-cleaner", Role.CLEANER);
        Property property = properties.save(Property.builder().manager(manager).name("Issue test").build());
        Long cleaningId = cleanings.save(Cleaning.builder().manager(manager).cleaner(cleaner).property(property).build())
                .getId();
        Long propertyId = property.getId();

        mvc.perform(post("/issues").with(as(cleaner))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"cleaningId": %d, "issueType": "DAMAGE", "description": "Broken chair"}
                                """.formatted(cleaningId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.propertyId").value(propertyId));

        mvc.perform(delete("/cleanings/{id}", cleaningId).with(as(manager)))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        assertThat(cleanings.existsById(cleaningId)).isFalse();
        mvc.perform(get("/issues").with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].propertyId").value(propertyId))
                .andExpect(jsonPath("$[0].description").value("Broken chair"));

        mvc.perform(delete("/properties/{id}", propertyId).with(as(manager)))
                .andExpect(status().isNoContent());
        entityManager.flush();
        entityManager.clear();

        assertThat(properties.existsById(propertyId)).isFalse();
        mvc.perform(get("/issues").with(as(manager)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private User saveUser(String username, Role role) {
        return users.save(User.builder().username(username).email(username + "@example.com").role(role).build());
    }

    private static RequestPostProcessor as(User user) {
        return authentication(UsernamePasswordAuthenticationToken.authenticated(user, null, user.getAuthorities()));
    }
}
