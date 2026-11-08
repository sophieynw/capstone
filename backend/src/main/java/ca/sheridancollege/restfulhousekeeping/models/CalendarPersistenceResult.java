package ca.sheridancollege.restfulhousekeeping.models;

import java.util.List;

public record CalendarPersistenceResult(
	int created,
	int skipped,
	List<Long> cleaningIds
) {
}
