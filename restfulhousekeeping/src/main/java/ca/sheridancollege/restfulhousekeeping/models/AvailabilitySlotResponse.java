package ca.sheridancollege.restfulhousekeeping.models;

import ca.sheridancollege.restfulhousekeeping.beans.AvailabilitySlot;
import ca.sheridancollege.restfulhousekeeping.beans.DayOfWeek;

import java.time.LocalTime;

public record AvailabilitySlotResponse(DayOfWeek dayOfWeek, LocalTime startTime, LocalTime endTime) {
    public static AvailabilitySlotResponse fromSlot(AvailabilitySlot slot) {
        return new AvailabilitySlotResponse(slot.getDayOfWeek(), slot.getStartTime(), slot.getEndTime());
    }
}
