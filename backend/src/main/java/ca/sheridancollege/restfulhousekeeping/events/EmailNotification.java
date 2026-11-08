package ca.sheridancollege.restfulhousekeeping.events;

public record EmailNotification(
        String to,
        String subject,
        String body
) {}
