package ca.sheridancollege.restfulhousekeeping.services;

import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import ca.sheridancollege.restfulhousekeeping.beans.Organization;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.AuthenticationRequest;
import ca.sheridancollege.restfulhousekeeping.models.AuthenticationResponse;
import ca.sheridancollege.restfulhousekeeping.models.RegisterRequest;
import ca.sheridancollege.restfulhousekeeping.models.UserResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.OrganizationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class AuthenticationResponseService {
	
	private UserRepository userRepository;
	private OrganizationRepository organizationRepository; 
	private PasswordEncoder passwordEncoder;
	private JwtService jwtService;
	private AuthenticationManager authenticationManager;
	private InvitationService invitationService;
	
	// a method to register a new user in our DB and generate a JWT for them
	@SuppressWarnings("unchecked")
	@Transactional
	public AuthenticationResponse register(RegisterRequest request) {
		String username = UsernamePolicy.normalizeAndValidate(request.getUsername());
		String email = EmailPolicy.normalizeAndValidate(request.getEmail());
		if (userRepository.existsByUsernameIgnoreCase(username)) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Username is already taken."
				);
		}
		if (userRepository.existsByEmailIgnoreCase(email)) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"Email is already in use."
				);
		}
		
		Organization organization = resolveOrganization(request);
		User user = User.builder()
				.firstName(request.getFirstName())
				.lastName(request.getLastName())
				.username(username)
				.email(email)
				.password(passwordEncoder.encode(request.getPassword()))
				.phoneNumber(request.getPhoneNumber())
				.role(request.getRole())
				.organization(organization)
				.build();
		userRepository.save(user);
		invitationService.reconcilePendingInvitations(user);
		var jwtToken = jwtService.generateToken(user);
		return AuthenticationResponse.builder()
				.token(jwtToken)
				.user(toUserResponse(user))
				.build();
	}
	
	// a method to authenticate an existing user and generate a JWT for them
	public AuthenticationResponse authenticate(AuthenticationRequest request) {
		String username = UsernamePolicy.normalizeRequired(request.getUsername());
		authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(
				username, request.getPassword()));
		User user = userRepository.findByUsernameIgnoreCase(username).orElseThrow();
		var jwtToken = jwtService.generateToken(user);
		return AuthenticationResponse.builder()
				.token(jwtToken)
				.user(toUserResponse(user))
				.build();
	}

	private UserResponse toUserResponse(User user) {
		return UserResponse.builder()
				.id(user.getId())
				.organization(user.getOrganization())
				.firstName(user.getFirstName())
				.lastName(user.getLastName())
				.username(user.getUsername())
				.email(user.getEmail())
				.phoneNumber(user.getPhoneNumber())
				.role(user.getRole())
				.build();
	}
	
	// get or create organization
	private Organization resolveOrganization(RegisterRequest request) {
		if (request.getRole() == null) {
			throw new ResponseStatusException(
					HttpStatus.BAD_REQUEST,
					"Account type is required"
			);
		}

	    String organizationName = request.getOrganizationName();

	    boolean hasName =
	            organizationName != null && !organizationName.isBlank();

		if (request.getRole() == Role.CLEANER) {
			if (hasName) {
				throw new ResponseStatusException(
						HttpStatus.BAD_REQUEST,
						"Cleaners cannot join an organization during signup"
				);
			}
			return null;
		}

	    if (!hasName) {
	        throw new ResponseStatusException(
	                HttpStatus.BAD_REQUEST,
	                "An organization name is required"
	        );
	    }

	    Organization organization = Organization.builder()
	            .name(organizationName.trim())
	            .description(null)
	            .build();

	    return organizationRepository.save(organization);
	}

}
