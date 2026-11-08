package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.RegisterRequest;
import ca.sheridancollege.restfulhousekeeping.repositories.OrganizationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AuthenticationResponseServiceTests {

    @Mock UserRepository userRepository;
    @Mock OrganizationRepository organizationRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock InvitationService invitationService;

    private AuthenticationResponseService service;

    @BeforeEach
    void setUp() {
        service = new AuthenticationResponseService(
                userRepository,
                organizationRepository,
                passwordEncoder,
                jwtService,
                authenticationManager,
                invitationService);
    }

    @Test
    void registersCleanerWithoutOrganization() {
        stubSuccessfulRegistration();
        service.register(cleanerRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getOrganization()).isNull();
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("cleaner@example.com");
        verify(organizationRepository, never()).findById(any());
    }

    @Test
    void delegatesPendingInvitationReconciliationAfterSavingUser() {
        stubSuccessfulRegistration();

        service.register(cleanerRequest());

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        verify(invitationService).reconcilePendingInvitations(userCaptor.getValue());
    }

    @Test
    void rejectsDuplicateEmailIgnoringCase() {
        when(userRepository.existsByEmailIgnoreCase("cleaner@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.register(cleanerRequest()))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT))
                .hasMessageContaining("Email is already in use");

        verify(userRepository, never()).save(any());
    }

    private RegisterRequest cleanerRequest() {
        return RegisterRequest.builder()
                .firstName("Casey")
                .lastName("Cleaner")
                .username("casey")
                .email(" Cleaner@Example.com ")
                .password("password")
                .phoneNumber("555-555-5555")
                .role(Role.CLEANER)
                .build();
    }

    private void stubSuccessfulRegistration() {
        when(passwordEncoder.encode("password")).thenReturn("encoded");
        when(jwtService.generateToken(any(User.class))).thenReturn("token");
    }
}
