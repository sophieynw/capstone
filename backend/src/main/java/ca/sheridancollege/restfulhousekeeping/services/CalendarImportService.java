package ca.sheridancollege.restfulhousekeeping.services;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.PropertyCalendarIntegration;
import ca.sheridancollege.restfulhousekeeping.exceptions.CalendarImportException;
import ca.sheridancollege.restfulhousekeeping.models.CalendarImportResponse;
import ca.sheridancollege.restfulhousekeeping.models.CalendarParseResult;
import ca.sheridancollege.restfulhousekeeping.models.CalendarPersistenceResult;
import ca.sheridancollege.restfulhousekeeping.models.ImportedReservation;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyCalendarIntegrationRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CalendarImportService {

	private final PropertyRepository propertyRepository;
	private final PropertyCalendarIntegrationRepository integrationRepository;
	private final CalendarDownloadService downloadService;
	private final AirbnbCalendarParser calendarParser;
	private final CalendarImportPersistenceService persistenceService;

	public CalendarImportResponse importCalendar(
		Long propertyId,
		String icalUrl,
		String authenticatedUsername
	) {
		String normalizedUrl = validateCalendarUrl(icalUrl);

		Property property = propertyRepository.findById(propertyId)
			.orElseThrow(() -> new CalendarImportException(
				HttpStatus.NOT_FOUND,
				"Property not found: " + propertyId
			));
		if (property.getManager() == null
			|| !Objects.equals(property.getManager().getUsername(), authenticatedUsername)) {
			throw new CalendarImportException(
				HttpStatus.FORBIDDEN,
				"You can only import a calendar for a property you manage"
			);
		}

		CalendarImportResponse response = runImport(property, normalizedUrl);
		recordSuccessfulConnection(property, normalizedUrl);
		return response;
	}

	public CalendarImportResponse syncStoredCalendar(Long integrationId) {
		PropertyCalendarIntegration integration = integrationRepository
			.findById(integrationId)
			.orElseThrow(() -> new CalendarImportException(
				HttpStatus.NOT_FOUND,
				"Calendar integration not found: " + integrationId
			));
		if (!Boolean.TRUE.equals(integration.getEnabled())) {
			throw new CalendarImportException(
				HttpStatus.CONFLICT,
				"Calendar integration is disabled"
			);
		}

		try {
			CalendarImportResponse response = runImport(
				integration.getProperty(),
				integration.getCalendarUrl()
			);
			markSyncSuccessful(integration);
			return response;
		} catch (RuntimeException exception) {
			markSyncFailed(integration, exception);
			throw exception;
		}
	}

	private CalendarImportResponse runImport(Property property, String icalUrl) {
		byte[] calendarBytes = downloadService.download(icalUrl);
		CalendarParseResult parsed = calendarParser.parse(calendarBytes);
		LocalDate today = LocalDate.now();
		List<ImportedReservation> currentReservations = parsed.reservations().stream()
			.filter(reservation -> !reservation.checkOutDate().isBefore(today))
			.toList();
		CalendarPersistenceResult persisted = persistenceService.saveReservations(
			property.getId(),
			currentReservations
		);

		int reservationCount = parsed.reservations().size();
		int pastReservationCount = reservationCount - currentReservations.size();
		return new CalendarImportResponse(
			property.getId(),
			parsed.eventsFound(),
			reservationCount,
			persisted.created(),
			persisted.skipped(),
			pastReservationCount,
			parsed.eventsFound() - reservationCount,
			persisted.cleaningIds()
		);
	}

	private String validateCalendarUrl(String icalUrl) {
		if (icalUrl == null || icalUrl.isBlank()) {
			throw new CalendarImportException(
				HttpStatus.BAD_REQUEST,
				"icalUrl is required"
			);
		}

		String normalizedUrl = icalUrl.trim();
		if (normalizedUrl.length() > 2000) {
			throw new CalendarImportException(
				HttpStatus.BAD_REQUEST,
				"icalUrl must not exceed 2000 characters"
			);
		}
		return normalizedUrl;
	}

	private void recordSuccessfulConnection(Property property, String calendarUrl) {
		PropertyCalendarIntegration integration = integrationRepository
			.findByProperty_Id(property.getId())
			.orElseGet(() -> PropertyCalendarIntegration.builder()
				.property(property)
				.build());

		integration.setCalendarUrl(calendarUrl);
		integration.setEnabled(true);
		markSyncSuccessful(integration);
		property.setCalendarIntegration(integration);
	}

	private void markSyncSuccessful(PropertyCalendarIntegration integration) {
		LocalDateTime now = LocalDateTime.now();
		integration.setLastSyncAttemptAt(now);
		integration.setLastSuccessfulSyncAt(now);
		integration.setLastSyncError(null);
		integrationRepository.save(integration);
	}

	private void markSyncFailed(
		PropertyCalendarIntegration integration,
		RuntimeException exception
	) {
		integration.setLastSyncAttemptAt(LocalDateTime.now());
		String message = exception.getMessage() == null
			? exception.getClass().getSimpleName()
			: exception.getMessage();
		integration.setLastSyncError(
			message.substring(0, Math.min(message.length(), 2000))
		);
		integrationRepository.save(integration);
	}
}
