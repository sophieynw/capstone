package ca.sheridancollege.restfulhousekeeping.models;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotNull;

public record UpdateCleaningScheduleRequest(
        @NotNull LocalDateTime dateTimeStart,
        @NotNull LocalDateTime dateTimeEnd
) {
}
