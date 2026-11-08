package ca.sheridancollege.restfulhousekeeping.models;

import jakarta.validation.constraints.NotBlank;

public record CreateCleaningChecklistItemRequest(
        @NotBlank String description
) {
}
