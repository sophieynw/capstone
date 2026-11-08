package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Proxy;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.PropertyCalendarIntegration;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CalendarImportResponse;
import ca.sheridancollege.restfulhousekeeping.models.CalendarParseResult;
import ca.sheridancollege.restfulhousekeeping.models.CalendarPersistenceResult;
import ca.sheridancollege.restfulhousekeeping.models.ImportedReservation;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyCalendarIntegrationRepository;

class CalendarImportServiceTests {

	@Test
	void ignoresPastReservationsBeforePersistence() {
		Property property = Property.builder()
			.id(7L)
			.manager(User.builder().username("sophie").role(Role.MANAGER).build())
			.build();
		byte[] bytes = new byte[] {1};
		LocalDate today = LocalDate.now();
		ImportedReservation past = reservation("past", today.minusDays(2));
		ImportedReservation future = reservation("future", today.plusDays(2));
		AtomicReference<List<ImportedReservation>> savedReservations =
			new AtomicReference<>();
		AtomicReference<PropertyCalendarIntegration> savedIntegration =
			new AtomicReference<>();

		PropertyRepository propertyRepository = (PropertyRepository) Proxy.newProxyInstance(
			PropertyRepository.class.getClassLoader(),
			new Class<?>[] {PropertyRepository.class},
			(proxy, method, args) -> {
				if (method.getName().equals("findById")) {
					return Optional.of(property);
				}
				throw new UnsupportedOperationException(method.getName());
			}
		);
		PropertyCalendarIntegrationRepository integrationRepository =
			(PropertyCalendarIntegrationRepository) Proxy.newProxyInstance(
				PropertyCalendarIntegrationRepository.class.getClassLoader(),
				new Class<?>[] {PropertyCalendarIntegrationRepository.class},
				(proxy, method, args) -> {
					if (method.getName().equals("findByProperty_Id")) {
						return Optional.empty();
					}
					if (method.getName().equals("save")) {
						PropertyCalendarIntegration integration =
							(PropertyCalendarIntegration) args[0];
						savedIntegration.set(integration);
						return integration;
					}
					throw new UnsupportedOperationException(method.getName());
				}
			);
		CalendarDownloadService downloadService = new CalendarDownloadService() {
			@Override
			public byte[] download(String calendarUrl) {
				return bytes;
			}
		};
		AirbnbCalendarParser parser = new AirbnbCalendarParser() {
			@Override
			public CalendarParseResult parse(byte[] calendarBytes) {
				return new CalendarParseResult(3, List.of(past, future));
			}
		};
		CalendarImportPersistenceService persistenceService =
			new CalendarImportPersistenceService(null, null, null) {
				@Override
				public CalendarPersistenceResult saveReservations(
					Long propertyId,
					List<ImportedReservation> reservations
				) {
					savedReservations.set(reservations);
					return new CalendarPersistenceResult(1, 0, List.of(99L));
				}
			};
		CalendarImportService service = new CalendarImportService(
			propertyRepository,
			integrationRepository,
			downloadService,
			parser,
			persistenceService
		);

		CalendarImportResponse response = service.importCalendar(
			7L,
			"https://www.airbnb.com/test.ics",
			"sophie"
		);

		assertThat(savedReservations.get())
			.extracting(ImportedReservation::eventUid)
			.containsExactly("future");
		assertThat(response.pastReservationsIgnored()).isEqualTo(1);
		assertThat(response.eventsIgnored()).isEqualTo(1);
		assertThat(savedIntegration.get().getCalendarUrl())
			.isEqualTo("https://www.airbnb.com/test.ics");
		assertThat(savedIntegration.get().getEnabled()).isTrue();
		assertThat(savedIntegration.get().getLastSuccessfulSyncAt()).isNotNull();
	}

	private ImportedReservation reservation(String uid, LocalDate checkOutDate) {
		return new ImportedReservation(
			uid,
			uid,
			"https://www.airbnb.com/hosting/reservations/details/" + uid,
			checkOutDate.minusDays(2),
			checkOutDate
		);
	}
}
