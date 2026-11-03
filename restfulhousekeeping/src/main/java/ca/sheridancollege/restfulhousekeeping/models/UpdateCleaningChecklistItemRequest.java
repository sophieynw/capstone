package ca.sheridancollege.restfulhousekeeping.models;

import jakarta.validation.constraints.NotNull;

public record UpdateCleaningChecklistItemRequest(
        @NotNull Boolean isComplete
) {}