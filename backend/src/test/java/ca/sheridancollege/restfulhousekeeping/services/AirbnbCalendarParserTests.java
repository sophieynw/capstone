package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import ca.sheridancollege.restfulhousekeeping.models.CalendarParseResult;
import ca.sheridancollege.restfulhousekeeping.models.ImportedReservation;

class AirbnbCalendarParserTests {

	private final AirbnbCalendarParser parser = new AirbnbCalendarParser();

	@Test
	void parsesReservationAndIgnoresNonReservationBlock() throws IOException {
		byte[] calendarBytes = getClass()
			.getResourceAsStream("/calendars/airbnb-calendar.ics")
			.readAllBytes();

		CalendarParseResult result = parser.parse(calendarBytes);

		assertThat(result.eventsFound()).isEqualTo(2);
		assertThat(result.reservations()).hasSize(1);

		ImportedReservation reservation = result.reservations().getFirst();
		assertThat(reservation.eventUid())
			.isEqualTo("reservation-HM9P35P5EJ@airbnb.com");
		assertThat(reservation.reservationCode()).isEqualTo("HM9P35P5EJ");
		assertThat(reservation.checkInDate()).isEqualTo(LocalDate.of(2026, 10, 31));
		assertThat(reservation.checkOutDate()).isEqualTo(LocalDate.of(2026, 11, 6));
	}
}
