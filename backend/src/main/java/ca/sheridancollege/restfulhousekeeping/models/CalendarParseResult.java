package ca.sheridancollege.restfulhousekeeping.models;

import java.util.List;

public record CalendarParseResult(
	int eventsFound,
	List<ImportedReservation> reservations
) {
}
