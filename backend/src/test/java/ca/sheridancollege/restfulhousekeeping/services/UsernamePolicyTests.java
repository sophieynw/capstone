package ca.sheridancollege.restfulhousekeeping.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;

class UsernamePolicyTests {

	@ParameterizedTest
	@ValueSource(strings = { "Sophie", " first.last ", "first_last", "first-last", "a.1-b_c" })
	void normalizeAndValidateAcceptsSupportedUsernames(String username) {
		assertEquals(
				username.trim().toLowerCase(),
				UsernamePolicy.normalizeAndValidate(username)
		);
	}

	@ParameterizedTest
	@ValueSource(strings = { ".sophie", "sophie_", "-sophie", "sophie-", "soph!e", "soph ie" })
	void normalizeAndValidateRejectsInvalidUsernames(String username) {
		ResponseStatusException exception = assertThrows(
				ResponseStatusException.class,
				() -> UsernamePolicy.normalizeAndValidate(username)
		);

		assertEquals(UsernamePolicy.FORMAT_ERROR, exception.getReason());
	}
}
