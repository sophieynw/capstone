package ca.sheridancollege.restfulhousekeeping.models;

import java.time.Instant;

public record CleaningNoteResponse(
        Long id,
        String authorFirstName,
        String authorLastName,
        String content,
        Instant createdAt
) {}
