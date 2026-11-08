package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.events.EmailNotification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.MailException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationListener {

    private final EmailService emailService;

    @TransactionalEventListener(
            phase = TransactionPhase.AFTER_COMMIT,
            fallbackExecution = true)
    public void send(EmailNotification notification) {
        try {
            emailService.sendEmail(
                    notification.to(),
                    notification.subject(),
                    notification.body());
        } catch (MailException exception) {
            log.error(
                    "Could not send '{}' email to {}",
                    notification.subject(),
                    notification.to(),
                    exception);
        }
    }
}
