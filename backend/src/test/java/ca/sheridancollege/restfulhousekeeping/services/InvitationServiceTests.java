package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import ca.sheridancollege.restfulhousekeeping.beans.Invitation;
import ca.sheridancollege.restfulhousekeeping.beans.InvitationStatus;
import ca.sheridancollege.restfulhousekeeping.beans.Organization;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.InvitationResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.InvitationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class InvitationServiceTests {

    @Mock InvitationRepository invitationRepository;
    @Mock UserRepository userRepository;
    @Mock InvitationNotificationService notificationService;

    InvitationService service;
    Organization organization;
    User manager;
    User cleaner;

    @BeforeEach
    void setup() {
        service = new InvitationService(
                invitationRepository,
                userRepository,
                notificationService);
        organization = Organization.builder().id(10L).name("Test Team").build();
        manager = User.builder()
                .id(20L)
                .firstName("Maya")
                .lastName("Manager")
                .username("manager")
                .email("manager@example.com")
                .role(Role.MANAGER)
                .organization(organization)
                .build();
        cleaner = User.builder()
                .id(30L)
                .firstName("Casey")
                .lastName("Cleaner")
                .username("cleaner")
                .email("cleaner@example.com")
                .role(Role.CLEANER)
                .build();
    }

    @Test
    void createsInvitationByNormalizedEmail() {
        when(userRepository.findByEmailIgnoreCase("cleaner@example.com"))
                .thenReturn(Optional.of(cleaner));
        when(invitationRepository
                .findByInvitedEmailIgnoreCaseAndOrganizationId(
                        cleaner.getEmail(), organization.getId()))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class)))
                .thenAnswer(invocation -> {
                    Invitation invitation = invocation.getArgument(0);
                    invitation.setId(40L);
                    return invitation;
                });

        InvitationResponse response = service.createInvitation(
                " Cleaner@Example.com ", manager);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(response.organizationId()).isEqualTo(organization.getId());
        verify(notificationService).invitationSent(
                "cleaner@example.com",
                cleaner,
                manager,
                organization);
    }

    @Test
    void resendsEmailWithoutDuplicatingExistingPendingInvitation() {
        Invitation existing = invitation(40L, InvitationStatus.PENDING);
        when(userRepository.findByEmailIgnoreCase(cleaner.getEmail()))
                .thenReturn(Optional.of(cleaner));
        when(invitationRepository
                .findByInvitedEmailIgnoreCaseAndOrganizationId(
                        cleaner.getEmail(), organization.getId()))
                .thenReturn(Optional.of(existing));

        InvitationResponse response = service.createInvitation(cleaner.getEmail(), manager);

        assertThat(response.id()).isEqualTo(existing.getId());
        verify(invitationRepository, never()).save(any(Invitation.class));
        verify(notificationService).invitationSent(
                cleaner.getEmail(),
                cleaner,
                manager,
                organization);
    }

    @Test
    void createsInvitationAndSendsRegistrationEmailWhenAccountDoesNotExist() {
        when(userRepository.findByEmailIgnoreCase("future@example.com"))
                .thenReturn(Optional.empty());
        when(invitationRepository
                .findByInvitedEmailIgnoreCaseAndOrganizationId(
                        "future@example.com", organization.getId()))
                .thenReturn(Optional.empty());
        when(invitationRepository.save(any(Invitation.class)))
                .thenAnswer(invocation -> {
                    Invitation invitation = invocation.getArgument(0);
                    invitation.setId(42L);
                    return invitation;
                });

        InvitationResponse response = service.createInvitation("future@example.com", manager);

        assertThat(response.id()).isEqualTo(42L);
        assertThat(response.cleanerId()).isNull();
        assertThat(response.cleanerName()).isNull();
        assertThat(response.invitedEmail()).isEqualTo("future@example.com");
        verify(notificationService).invitationSent(
                "future@example.com",
                null,
                manager,
                organization);
    }

    @Test
    void rejectsCleanerWhoAlreadyBelongsToAnOrganization() {
        cleaner.setOrganization(Organization.builder().id(99L).build());
        when(userRepository.findByEmailIgnoreCase(cleaner.getEmail()))
                .thenReturn(Optional.of(cleaner));

        assertThatThrownBy(() -> service.createInvitation(cleaner.getEmail(), manager))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(exception.getReason())
                            .isEqualTo("This cleaner is already part of an organization");
                });
    }

    @Test
    void acceptingInvitationDeclinesEveryOtherPendingInvitation() {
        Invitation accepted = invitation(40L, InvitationStatus.PENDING);
        User otherManager = User.builder()
                .id(21L)
                .firstName("Morgan")
                .lastName("Manager")
                .email("other-manager@example.com")
                .role(Role.MANAGER)
                .build();
        Organization otherOrganization = Organization.builder()
                .id(11L)
                .name("Other Team")
                .build();
        otherManager.setOrganization(otherOrganization);
        Invitation other = invitation(
                41L, InvitationStatus.PENDING, otherManager, otherOrganization);
        when(invitationRepository.findById(accepted.getId()))
                .thenReturn(Optional.of(accepted));
        when(userRepository.findById(cleaner.getId())).thenReturn(Optional.of(cleaner));
        when(invitationRepository.save(any(Invitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(invitationRepository.findByCleanerIdAndStatus(
                cleaner.getId(), InvitationStatus.PENDING))
                .thenReturn(List.of(accepted, other));

        service.acceptInvitation(accepted.getId(), cleaner);

        assertThat(accepted.getStatus()).isEqualTo(InvitationStatus.ACCEPTED);
        assertThat(other.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        assertThat(cleaner.getOrganization()).isEqualTo(organization);
        verify(notificationService).invitationResponded(
                accepted, cleaner, InvitationStatus.ACCEPTED);
        verify(notificationService).invitationResponded(
                other, cleaner, InvitationStatus.DECLINED);
    }

    @Test
    void decliningInvitationDeclinesEveryOtherPendingInvitation() {
        Invitation declined = invitation(40L, InvitationStatus.PENDING);
        User otherManager = User.builder()
                .id(21L)
                .firstName("Morgan")
                .lastName("Manager")
                .email("other-manager@example.com")
                .role(Role.MANAGER)
                .build();
        Organization otherOrganization = Organization.builder()
                .id(11L)
                .name("Other Team")
                .build();
        otherManager.setOrganization(otherOrganization);
        Invitation other = invitation(
                41L, InvitationStatus.PENDING, otherManager, otherOrganization);
        when(invitationRepository.findById(declined.getId()))
                .thenReturn(Optional.of(declined));
        when(invitationRepository.save(any(Invitation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(invitationRepository.findByCleanerIdAndStatus(
                cleaner.getId(), InvitationStatus.PENDING))
                .thenReturn(List.of(declined, other));

        service.declineInvitation(declined.getId(), cleaner);

        assertThat(declined.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        assertThat(other.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        verify(notificationService).invitationResponded(
                declined, cleaner, InvitationStatus.DECLINED);
        verify(notificationService).invitationResponded(
                other, cleaner, InvitationStatus.DECLINED);
    }

    @Test
    void reusesExistingInvitationRecordAfterItIsDeclined() {
        Invitation existing = invitation(40L, InvitationStatus.DECLINED);
        when(userRepository.findByEmailIgnoreCase(cleaner.getEmail()))
                .thenReturn(Optional.of(cleaner));
        when(invitationRepository.findByInvitedEmailIgnoreCaseAndOrganizationId(
                cleaner.getEmail(), organization.getId()))
                .thenReturn(Optional.of(existing));
        when(invitationRepository.save(existing)).thenReturn(existing);

        InvitationResponse response = service.createInvitation(cleaner.getEmail(), manager);

        assertThat(response.id()).isEqualTo(40L);
        assertThat(existing.getStatus()).isEqualTo(InvitationStatus.PENDING);
        verify(invitationRepository).save(existing);
    }

    @Test
    void reconcilesManagerSignupByDecliningAndNotifyingPendingInvitations() {
        User registeringManager = User.builder()
                .id(50L)
                .firstName("Taylor")
                .lastName("Manager")
                .email(cleaner.getEmail())
                .role(Role.MANAGER)
                .organization(Organization.builder().id(12L).name("New Team").build())
                .build();
        Invitation pending = invitation(40L, InvitationStatus.PENDING);
        when(invitationRepository.findByInvitedEmailIgnoreCaseAndStatus(
                cleaner.getEmail(), InvitationStatus.PENDING))
                .thenReturn(List.of(pending));

        service.reconcilePendingInvitations(registeringManager);

        assertThat(pending.getStatus()).isEqualTo(InvitationStatus.DECLINED);
        verify(notificationService).invitationResponded(
                pending, registeringManager, InvitationStatus.DECLINED);
    }

    private Invitation invitation(Long id, InvitationStatus status) {
        return invitation(id, status, manager, organization);
    }

    private Invitation invitation(
            Long id,
            InvitationStatus status,
            User invitingManager,
            Organization invitingOrganization) {

        return Invitation.builder()
                .id(id)
                .cleaner(cleaner)
                .invitedEmail(cleaner.getEmail())
                .organization(invitingOrganization)
                .invitedBy(invitingManager)
                .status(status)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
