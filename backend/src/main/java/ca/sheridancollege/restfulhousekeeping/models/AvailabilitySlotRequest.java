package ca.sheridancollege.restfulhousekeeping.models;
import ca.sheridancollege.restfulhousekeeping.beans.DayOfWeek;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record AvailabilitySlotRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime) {}
