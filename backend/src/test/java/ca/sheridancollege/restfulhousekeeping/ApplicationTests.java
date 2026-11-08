package ca.sheridancollege.restfulhousekeeping;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
class ApplicationTests {

	@Autowired
	private CleaningRepository cleaningRepository;

	@Autowired
	private UserRepository userRepository;

	@Test
	void contextLoads() {
	}

	@Test
	@Transactional
	void demoSeedSchedulesTheFurnaceFilterAtItsRecurringInterval() {
		Long cleanerId = userRepository.findByUsernameIgnoreCase("robert").orElseThrow().getId();
		var completedCleanings = cleaningRepository.findByCleanerIdAndIsCompleteTrue(cleanerId);
		assertThat(completedCleanings).hasSize(1);
		Cleaning completed = completedCleanings.getFirst();
		LocalDateTime now = LocalDateTime.now();
		var today = now.toLocalDate();
		assertThat(completed.getDateTimeCompleted()).isBetween(today.atStartOfDay(), now);
		assertThat(cleaningRepository.findAll().stream()
				.filter(visit -> visit.getNotes() == null || !visit.getNotes().startsWith("Automatically generated cleaning")))
				.hasSize(3)
				.allSatisfy(visit -> assertThat(visit.getDateTimeStart().toLocalDate()).isEqualTo(today));
		assertThat(completed.getCleaningChecklistItems()).hasSize(5).allSatisfy(item -> {
			assertThat(item.getIsComplete()).isTrue();
			assertThat(item.getChecklistItem().getLastCompleted()).isEqualTo(completed.getDateTimeCompleted());
		});

		var upcoming = cleaningRepository.findPendingChecklistSchedule(completed.getProperty().getId(), today.atStartOfDay());
		assertThat(upcoming).hasSize(5);
		var nextDue = upcoming.getFirst().getDateTimeStart().toLocalDate();
		for (Cleaning visit : upcoming) {
			var day = visit.getDateTimeStart().toLocalDate();
			boolean due = !day.isBefore(nextDue);
			boolean scheduled = visit.getCleaningChecklistItems().stream()
					.anyMatch(item -> item.getChecklistItem().getDescription().equals("Replace furnace filter"));
			assertThat(scheduled).isEqualTo(due);
			if (due) {
				nextDue = day.plusDays(45);
			}
		}
	}

}
