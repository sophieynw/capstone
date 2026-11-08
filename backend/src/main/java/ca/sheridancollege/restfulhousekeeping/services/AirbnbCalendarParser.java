package ca.sheridancollege.restfulhousekeeping.services;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.Temporal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ca.sheridancollege.restfulhousekeeping.exceptions.CalendarImportException;
import ca.sheridancollege.restfulhousekeeping.models.CalendarParseResult;
import ca.sheridancollege.restfulhousekeeping.models.ImportedReservation;
import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtEnd;
import net.fortuna.ical4j.model.property.DtStart;
import net.fortuna.ical4j.model.property.Uid;

@Service
public class AirbnbCalendarParser {

	private static final Pattern RESERVATION_URL_PATTERN = Pattern.compile(
		"https://[^\\s]+/hosting/reservations/details/([A-Za-z0-9]+)",
		Pattern.CASE_INSENSITIVE
	);

	public CalendarParseResult parse(byte[] calendarBytes) {
		try (InputStream input = new ByteArrayInputStream(calendarBytes)) {
			net.fortuna.ical4j.model.Calendar calendar =
				new CalendarBuilder().build(input);

			List<VEvent> events = calendar.getComponents(Component.VEVENT);
			List<ImportedReservation> reservations = new ArrayList<>();

			for (VEvent event : events) {
				parseReservation(event).ifPresent(reservations::add);
			}

			return new CalendarParseResult(events.size(), reservations);
		} catch (IOException | ParserException exception) {
			throw new CalendarImportException(
				HttpStatus.BAD_REQUEST,
				"The downloaded file is not a valid iCalendar",
				exception
			);
		}
	}

	private Optional<ImportedReservation> parseReservation(VEvent event) {
		String description = event.<Description>getProperty(Property.DESCRIPTION)
			.map(Description::getValue)
			.orElse("");

		Matcher matcher = RESERVATION_URL_PATTERN.matcher(description);
		if (!matcher.find()) {
			return Optional.empty();
		}

		String uid = event.<Uid>getProperty(Property.UID)
			.map(Uid::getValue)
			.orElseThrow(() -> new CalendarImportException(
				HttpStatus.BAD_REQUEST,
				"A reservation event is missing its UID"
			));

		Temporal startValue = event.<DtStart<?>>getProperty(Property.DTSTART)
			.orElseThrow(() -> new CalendarImportException(
				HttpStatus.BAD_REQUEST,
				"A reservation event is missing DTSTART"
			))
			.getDate();

		Temporal endValue = event.<DtEnd<?>>getProperty(Property.DTEND)
			.orElseThrow(() -> new CalendarImportException(
				HttpStatus.BAD_REQUEST,
				"A reservation event is missing DTEND"
			))
			.getDate();

		LocalDate checkInDate = toLocalDate(startValue);
		LocalDate checkOutDate = toLocalDate(endValue);
		if (!checkOutDate.isAfter(checkInDate)) {
			throw new CalendarImportException(
				HttpStatus.BAD_REQUEST,
				"A reservation checkout date must be after its check-in date"
			);
		}

		return Optional.of(new ImportedReservation(
			uid,
			matcher.group(1),
			matcher.group(),
			checkInDate,
			checkOutDate
		));
	}

	private LocalDate toLocalDate(Temporal value) {
		if (value instanceof LocalDate date) {
			return date;
		}
		if (value instanceof LocalDateTime dateTime) {
			return dateTime.toLocalDate();
		}
		if (value instanceof ZonedDateTime dateTime) {
			return dateTime.toLocalDate();
		}
		if (value instanceof OffsetDateTime dateTime) {
			return dateTime.toLocalDate();
		}
		if (value instanceof Instant instant) {
			return instant.atOffset(ZoneOffset.UTC).toLocalDate();
		}

		throw new CalendarImportException(
			HttpStatus.BAD_REQUEST,
			"Unsupported calendar date format: " + value.getClass().getSimpleName()
		);
	}
}
