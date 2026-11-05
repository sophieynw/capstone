package ca.sheridancollege.restfulhousekeeping.services;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningSource;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.models.CalendarPersistenceResult;
import ca.sheridancollege.restfulhousekeeping.models.ImportedReservation;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.ChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;

@SpringBootTest
@Transactional
class CalendarImportPersistenceServiceTests {

	@Autowired
	private CalendarImportPersistenceService persistenceService;

	@Autowired
	private CleaningRepository cleaningRepository;

	@Autowired
	private ChecklistItemRepository checklistItemRepository;

	@Autowired
	private PropertyRepository propertyRepository;

	@Test
	void createsCleaningWithChecklistAndSkipsDuplicateEventUid() {
		LocalDate firstCheckout = LocalDate.now().plusDays(4);
		Property property = propertyRepository.save(Property.builder()
			.name("Calendar Import Test Property")
			.manager(propertyRepository.findById(1L).orElseThrow().getManager())
			.build());
		ChecklistItem monthlyTask = checklistItemRepository.save(ChecklistItem.builder()
			.property(property)
			.description("Monthly test task")
			.frequencyDays(30)
			.build());
		ImportedReservation reservation = new ImportedReservation(
			"test-reservation-uid@airbnb.com",
			"TESTCODE123",
			"https://www.airbnb.com/hosting/reservations/details/TESTCODE123",
			firstCheckout.minusDays(2),
			firstCheckout
		);
		ImportedReservation laterReservation = new ImportedReservation(
			"later-test-reservation-uid@airbnb.com",
			"TESTCODE456",
			"https://www.airbnb.com/hosting/reservations/details/TESTCODE456",
			firstCheckout.plusDays(1),
			firstCheckout.plusDays(3)
		);

		CalendarPersistenceResult first = persistenceService.saveReservations(
			property.getId(),
			List.of(laterReservation, reservation)
		);
		CalendarPersistenceResult second = persistenceService.saveReservations(
			property.getId(),
			List.of(reservation)
		);

		assertThat(first.created()).isEqualTo(2);
		assertThat(first.skipped()).isZero();
		assertThat(second.created()).isZero();
		assertThat(second.skipped()).isEqualTo(1);

		Cleaning cleaning = cleaningRepository.findById(first.cleaningIds().getLast())
			.orElseThrow();
		assertThat(cleaning.getSource()).isEqualTo(CleaningSource.AIRBNB_ICAL);
		assertThat(cleaning.getCleaner()).isNull();
		assertThat(cleaning.getIsComplete()).isFalse();
		assertThat(cleaning.getDateTimeStart())
			.isEqualTo(firstCheckout.atTime(11, 0));
		assertThat(cleaning.getDateTimeEnd())
			.isEqualTo(firstCheckout.atTime(16, 0));
		assertThat(cleaning.getCleaningChecklistItems())
			.anyMatch(item -> monthlyTask.getId().equals(item.getChecklistItem().getId()));
		Cleaning laterCleaning = cleaningRepository.findById(first.cleaningIds().getFirst()).orElseThrow();
		assertThat(laterCleaning.getCleaningChecklistItems())
			.noneMatch(item -> monthlyTask.getId().equals(item.getChecklistItem().getId()));
	}
}
