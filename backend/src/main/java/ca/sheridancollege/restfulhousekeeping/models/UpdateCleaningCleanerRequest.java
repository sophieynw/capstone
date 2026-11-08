package ca.sheridancollege.restfulhousekeeping.models;

import jakarta.validation.constraints.Positive;

public record UpdateCleaningCleanerRequest(
        @Positive Long cleanerId
) {
}
