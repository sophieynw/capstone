package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import ca.sheridancollege.restfulhousekeeping.events.EmailNotification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
class EmailNotificationListenerTests {

    @Mock EmailService emailService;

    private EmailNotificationListener listener;
    private EmailNotification notification;

    @BeforeEach
    void setUp() {
        listener = new EmailNotificationListener(emailService);
        notification = new EmailNotification(
                "manager@example.com", "Subject", "Body");
    }

    @Test
    void sendsPublishedEmail() {
        listener.send(notification);

        verify(emailService).sendEmail(
                notification.to(), notification.subject(), notification.body());
    }

    @Test
    void mailFailureDoesNotUndoCompletedBusinessTransaction() {
        doThrow(new MailSendException("Mail server unavailable"))
                .when(emailService)
                .sendEmail(notification.to(), notification.subject(), notification.body());

        assertThatCode(() -> listener.send(notification)).doesNotThrowAnyException();
    }
}
