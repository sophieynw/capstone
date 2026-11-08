package ca.sheridancollege.restfulhousekeeping.controllers;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import ca.sheridancollege.restfulhousekeeping.models.AuthenticationRequest;
import ca.sheridancollege.restfulhousekeeping.models.AuthenticationResponse;
import ca.sheridancollege.restfulhousekeeping.models.RegisterRequest;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import ca.sheridancollege.restfulhousekeeping.services.AuthenticationResponseService;
import ca.sheridancollege.restfulhousekeeping.services.UsernamePolicy;
import lombok.AllArgsConstructor;

@RestController
@AllArgsConstructor
@RequestMapping("/api/v1/auth")
public class AuthenticationController {
	
	private final AuthenticationResponseService authenticationResponseService;
	private final UserRepository userRepository;
	
	// checks if username is available
	@GetMapping("/username-availability")
	public Map<String, Boolean> usernameAvailable(@RequestParam String username) {
		String normalizedUsername = UsernamePolicy.normalizeAndValidate(username);
		boolean available = !userRepository.existsByUsernameIgnoreCase(normalizedUsername);
		return Map.of("available", available);
	}

	// map incoming POST requests to register a new user
	@PostMapping(value = "/register", consumes = "application/json")
	public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
		return ResponseEntity.ok(authenticationResponseService.register(request));
	}
	
	// map incoming POST requests to authenticate an existing user
	@PostMapping(value = "/authenticate", consumes = "application/json")
	public ResponseEntity<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest request) {
		return ResponseEntity.ok(authenticationResponseService.authenticate(request));
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleResponseStatusException(
			ResponseStatusException exception
	) {
		String message = exception.getReason() == null
				? "The authentication request could not be completed."
				: exception.getReason();
		return ResponseEntity.status(exception.getStatusCode())
				.body(Map.of("message", message));
	}

	@ExceptionHandler(AuthenticationException.class)
	public ResponseEntity<Map<String, String>> handleAuthenticationException() {
		return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
				.body(Map.of("message", "Invalid username or password."));
	}

}
