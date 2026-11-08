package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import ca.sheridancollege.restfulhousekeeping.beans.Invitation;
import ca.sheridancollege.restfulhousekeeping.beans.InvitationStatus;
import ca.sheridancollege.restfulhousekeeping.beans.Organization;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.events.EmailNotification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InvitationNotificationServiceTests {

    @Mock ApplicationEventPublisher eventPublisher;

    private InvitationNotificationService service;
    private User manager;
    private User cleaner;
    private Organization organization;

    @BeforeEach
    void setUp() {
        service = new InvitationNotificationService(eventPublisher);
        organization = Organization.builder().id(1L).name("Test Team").build();
        manager = User.builder()
                .id(2L)
                .firstName("Maya")
                .lastName("Manager")
                .email("manager@example.com")
                .role(Role.MANAGER)
                .organization(organization)
                .build();
        cleaner = User.builder()
                .id(3L)
                .firstName("Casey")
                .lastName("Cleaner")
                .email("cleaner@example.com")
                .role(Role.CLEANER)
                .build();
    }

    @Test
    void publishesRegistrationInstructionsWhenCleanerHasNoAccount() {
        service.invitationSent("future@example.com", null, manager, organization);

        EmailNotification notification = capturedNotification();
        assertThat(notification.to()).isEqualTo("future@example.com");
        assertThat(notification.subject()).isEqualTo("RESTful Housekeeping: Invitation");
        assertThat(notification.body()).contains(
                "create a RESTful Housekeeping cleaner account with this email address");
    }

    @Test
    void publishesManagerResponseNotification() {
        Invitation invitation = Invitation.builder()
                .organization(organization)
                .invitedBy(manager)
                .build();

        service.invitationResponded(
                invitation, cleaner, InvitationStatus.ACCEPTED);

        EmailNotification notification = capturedNotification();
        assertThat(notification.to()).isEqualTo(manager.getEmail());
        assertThat(notification.subject())
                .isEqualTo("RESTful Housekeeping: Invitation Accepted");
        assertThat(notification.body()).contains(
                "Casey Cleaner has accepted your invitation to join Test Team");
    }

    private EmailNotification capturedNotification() {
        ArgumentCaptor<EmailNotification> captor =
                ArgumentCaptor.forClass(EmailNotification.class);
        verify(eventPublisher).publishEvent(captor.capture());
        return captor.getValue();
    }
}
