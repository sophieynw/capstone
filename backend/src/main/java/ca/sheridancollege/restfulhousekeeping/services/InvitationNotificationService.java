package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Invitation;
import ca.sheridancollege.restfulhousekeeping.beans.InvitationStatus;
import ca.sheridancollege.restfulhousekeeping.beans.Organization;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.events.EmailNotification;

import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InvitationNotificationService {

    private final ApplicationEventPublisher eventPublisher;

    public void invitationSent(
            String email,
            User cleaner,
            User manager,
            Organization organization) {

        String greeting = cleaner == null
                ? "Hello,"
                : "Hello " + cleaner.getFirstName() + ",";
        String nextStep = cleaner == null
                ? "Please create a RESTful Housekeeping cleaner account with this email "
                    + "address to review your invitation."
                : "Please open RESTful Housekeeping to review your invitation.";

        publish(
                email,
                "RESTful Housekeeping: Invitation",
                greeting + "\n\n"
                        + "You have been invited to work with "
                        + manager.getFirstName() + " " + manager.getLastName()
                        + " at " + organization.getName() + ".\n\n"
                        + nextStep + "\n\n"
                        + "Regards,\nRESTful Housekeeping");
    }

    public void invitationResponded(
            Invitation invitation,
            User cleaner,
            InvitationStatus response) {

        if (response != InvitationStatus.ACCEPTED
                && response != InvitationStatus.DECLINED) {
            throw new IllegalArgumentException(
                    "Invitation response email requires ACCEPTED or DECLINED status");
        }

        User manager = invitation.getInvitedBy();
        String responseText = response == InvitationStatus.ACCEPTED
                ? "accepted"
                : "declined";
        String subjectStatus = response == InvitationStatus.ACCEPTED
                ? "Accepted"
                : "Declined";

        publish(
                manager.getEmail(),
                "RESTful Housekeeping: Invitation " + subjectStatus,
                "Hello " + manager.getFirstName() + ",\n\n"
                        + cleaner.getFirstName() + " " + cleaner.getLastName()
                        + " has " + responseText + " your invitation to join "
                        + invitation.getOrganization().getName() + ".\n\n"
                        + "Regards,\nRESTful Housekeeping");
    }

    private void publish(String to, String subject, String body) {
        eventPublisher.publishEvent(new EmailNotification(to, subject, body));
    }
}
