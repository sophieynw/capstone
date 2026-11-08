package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class EmailPolicyTests {

    @Test
    void normalizesValidEmail() {
        assertThat(EmailPolicy.normalizeAndValidate(" Cleaner@Example.COM "))
                .isEqualTo("cleaner@example.com");
    }

    @Test
    void rejectsInvalidEmail() {
        assertThatThrownBy(() -> EmailPolicy.normalizeAndValidate("not-an-email"))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode())
                                .isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
