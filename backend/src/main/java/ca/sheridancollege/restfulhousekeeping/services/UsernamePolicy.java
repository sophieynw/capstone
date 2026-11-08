package ca.sheridancollege.restfulhousekeeping.services;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public final class UsernamePolicy {

	public static final String FORMAT_ERROR =
			"Username may contain only letters, numbers, periods, underscores, and hyphens, and must begin and end with a letter or number.";

	private static final int MIN_LENGTH = 3;
	private static final Pattern VALID_USERNAME =
			Pattern.compile("^[a-z0-9](?:[a-z0-9._-]*[a-z0-9])?$");

	private UsernamePolicy() {
	}

	public static String normalize(String username) {
		if (username == null) {
			return "";
		}

		return username.trim().toLowerCase(Locale.ROOT);
	}

	public static String normalizeRequired(String username) {
		String normalizedUsername = normalize(username);
		if (normalizedUsername.isEmpty()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username is required.");
		}

		return normalizedUsername;
	}

	public static String normalizeAndValidate(String username) {
		String normalizedUsername = normalizeRequired(username);
		if (normalizedUsername.length() < MIN_LENGTH) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Username must be at least 3 characters."
			);
		}
		if (!VALID_USERNAME.matcher(normalizedUsername).matches()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, FORMAT_ERROR);
		}

		return normalizedUsername;
	}
}
