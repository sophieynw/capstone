package ca.sheridancollege.restfulhousekeeping.services;

import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import ca.sheridancollege.restfulhousekeeping.beans.PropertyCalendarIntegration;
import ca.sheridancollege.restfulhousekeeping.models.CalendarImportResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyCalendarIntegrationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class CalendarImportScheduler {

	private final PropertyCalendarIntegrationRepository integrationRepository;
	private final CalendarImportService calendarImportService;

	@Scheduled(
		fixedDelayString = "${calendar.import.fixed-delay:PT1H}",
		initialDelayString = "${calendar.import.initial-delay:PT1M}"
	)
	public void importEnabledCalendars() {
		List<PropertyCalendarIntegration> integrations =
			integrationRepository.findAllByEnabledTrue();
		log.info(
			"Starting scheduled Airbnb calendar sync for {} integration(s)",
			integrations.size()
		);

		for (PropertyCalendarIntegration integration : integrations) {
			Long propertyId = integration.getProperty().getId();
			try {
				log.info("Syncing Airbnb calendar for property {}", propertyId);
				CalendarImportResponse response =
					calendarImportService.syncStoredCalendar(integration.getId());
				log.info(
					"Completed Airbnb calendar sync for property {}: "
						+ "{} cleaning(s) created, {} duplicate(s) skipped",
					propertyId,
					response.cleaningsCreated(),
					response.duplicatesSkipped()
				);
			} catch (RuntimeException exception) {
				log.warn(
					"Scheduled Airbnb calendar import failed for property {}: {}",
					propertyId,
					exception.getMessage()
				);
			}
		}

		log.info("Finished scheduled Airbnb calendar sync");
	}
}
