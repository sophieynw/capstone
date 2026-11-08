package ca.sheridancollege.restfulhousekeeping.models;

import java.time.LocalTime;

public record AvailabilitySlotUpdateRequest(LocalTime startTime, LocalTime endTime) {}
