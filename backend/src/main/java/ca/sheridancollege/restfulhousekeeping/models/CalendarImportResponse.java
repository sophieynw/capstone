package ca.sheridancollege.restfulhousekeeping.models;

import java.util.List;

public record CalendarImportResponse(
	Long propertyId,
	int eventsFound,
	int reservationsFound,
	int cleaningsCreated,
	int duplicatesSkipped,
	int pastReservationsIgnored,
	int eventsIgnored,
	List<Long> cleaningIds
) {
}
