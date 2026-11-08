package ca.sheridancollege.restfulhousekeeping.bootstrap;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

import ca.sheridancollege.restfulhousekeeping.services.RecurringChecklistService;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ca.sheridancollege.restfulhousekeeping.beans.Availability;
import ca.sheridancollege.restfulhousekeeping.beans.AvailabilitySlot;
import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.DayOfWeek;
import ca.sheridancollege.restfulhousekeeping.beans.Organization;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.repositories.AvailabilityRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.ChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.OrganizationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.SlotRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import lombok.AllArgsConstructor;

@Component
@AllArgsConstructor
@ConditionalOnProperty(name = "app.bootstrap.enabled", havingValue = "true")
public class BootstrapData implements CommandLineRunner {

	private final OrganizationRepository organizationRepository;
	private final UserRepository userRepository;
	private final PropertyRepository propertyRepository;
	private final CleaningRepository cleaningRepository;
	private final AvailabilityRepository availabilityRepository;
	private final ChecklistItemRepository checklistItemRepository;
	private final SlotRepository slotRepository;
	private final PasswordEncoder passwordEncoder;
	private final RecurringChecklistService recurringChecklistService;

	private void seedAvailability(User cleaner, DayOfWeek... days) {
		Availability availability = availabilityRepository.save(Availability.builder()
				.cleaner(cleaner)
				.build());
		for (DayOfWeek day : days) {
			slotRepository.save(AvailabilitySlot.builder()
					.availability(availability)
					.cleaner(cleaner)
					.dayOfWeek(day)
					.startTime(LocalTime.of(9, 0))
					.endTime(LocalTime.of(17, 0))
					.build());
		}
	}
	
	private ChecklistItem generateChecklistItem(Property property, String description, Integer frequencyDays, Integer completedDaysAgo) {
		return ChecklistItem.builder()
				.property(property)
				.description(description)
				.frequencyDays(frequencyDays)
				.lastCompleted(LocalDate.now().minusDays(completedDaysAgo).atStartOfDay())
				.build();
	}
		
	@Override
	@Transactional
	public void run(String... args) throws Exception {
		if (organizationRepository.count() > 0) {
			return;
		}
		
		// Organizations
		Organization organization1 = Organization.builder().name("The Everything Company")
				.description("We specialize in literally everything.").build();
		organization1 = organizationRepository.save(organization1);
		Organization organization2 = organizationRepository.save(Organization.builder()
				.name("Cedar Key Hosting")
				.description("Manages short-term rentals and coordinates guest turnovers.")
				.build());
		
		
		
		// People
		User manager1 = User.builder()
				.organization(organization1)
				.firstName("Sophie")
				.lastName("Wang")
				.username("sophie")
				.email("sophie@test.com")
				.password(passwordEncoder.encode("password"))
				.phoneNumber("6471231234")
				.profilePicturePath("https://ik.imagekit.io/restfulhousekeeping/restful-housekeeping/profile-pictures/sophie.jpg")
				.role(Role.MANAGER)
				.build();
		User cleaner1 = User.builder()
				.organization(organization1)
				.firstName("Robert")
				.lastName("Fleming")
				.username("robert")
				.email("robert@test.com")
				.password(passwordEncoder.encode("password"))
				.phoneNumber("9056083833")
				.profilePicturePath("https://ik.imagekit.io/restfulhousekeeping/restful-housekeeping/profile-pictures/robert.webp?updatedAt=1793860310886")
				.role(Role.CLEANER)
				.build();
		User cleaner2 = User.builder()
				.organization(organization1)
				.firstName("Katie")
				.lastName("McEwan")
				.username("katie")
				.email("katie@test.com")
				.password(passwordEncoder.encode("password"))
				.phoneNumber("2260001234")
				.profilePicturePath("https://ik.imagekit.io/restfulhousekeeping/restful-housekeeping/profile-pictures/katie.jpg")
				.role(Role.CLEANER)
				.build();
		User cleaner3 = User.builder()
				//.organization(organization1)
				.firstName("Alex")
				.lastName("Tumanan")
				.username("alex")
				.email("alex@test.com")
				.password(passwordEncoder.encode("password"))
				.phoneNumber("4160001234")
				.role(Role.CLEANER)
				.build();
		User manager2 = User.builder()
				.organization(organization2)
				.firstName("Sunggyu")
				.lastName("Kang")
				.username("sunggyu")
				.email("sunggyu@test.com")
				.password(passwordEncoder.encode("password"))
				.phoneNumber("6470001234")
				.role(Role.MANAGER)
				.build();
		manager1 = userRepository.save(manager1);
		cleaner1 = userRepository.save(cleaner1);
		cleaner2 = userRepository.save(cleaner2);
		cleaner3 = userRepository.save(cleaner3);
		userRepository.save(manager2);
		
		
		
		// Sophie's Properties
		Property property1 = Property.builder()
				.manager(manager1)
			    .name("Lakeshore Studio")
			    .propertyPicturePath("https://ik.imagekit.io/restfulhousekeeping/restful-housekeeping/property-pictures/lakeshore.jpg")
			    .street("2531 Lakeshore Blvd.")
			    .unit("G9")
			    .city("Etobicoke")
			    .province("ON")
			    .postalCode("L6D 7N4")
			    .country("Canada")
			    .accessInstructions("Puroma lockbox by the front door; code 1352.")
			    .build();
		Property property2 = Property.builder()
				.manager(manager1)
			    .name("Union 1BR Condo")
			    .propertyPicturePath("https://ik.imagekit.io/restfulhousekeeping/restful-housekeeping/property-pictures/union.jpg")
			    .street("101 Union St.")
			    .unit("292")
			    .city("Oakville")
			    .province("Ontario")
			    .postalCode("L4V 1V1")
			    .country("Canada")
			    .accessInstructions(
			        "Jump over the fence and take a right."
			    )
			    .build();
		Property property3 = Property.builder()
				.manager(manager1)
			    .name("Maple 5BR House")
			    .propertyPicturePath("https://ik.imagekit.io/restfulhousekeeping/restful-housekeeping/property-pictures/maple.webp")
			    .street("47 Maple Ridge Drive")
			    .city("Burlington")
			    .province("Ontario")
			    .postalCode("L7M 2P8")
			    .country("Canada")
			    .accessInstructions(
			        "Keypad code - 7722."
			    )
			    .build();
//		Property property4 = Property.builder()
//				.manager(manager1)
//			    .name("Harbourview Condo")
//			    .street("2500 Lakeshore Road West")
//			    .unit("604")
//			    .city("Oakville")
//			    .province("Ontario")
//			    .postalCode("L6L 1H8")
//			    .country("Canada")
//			    .accessInstructions(
//			        "Check in with the concierge and ask for the unit key."
//			    )
//			    .build();
//		Property property5 = Property.builder()
//				.manager(manager1)
//			    .name("Downtown Loft")
//			    .street("80 King Street West")
//			    .unit("1205")
//			    .city("Toronto")
//			    .province("Ontario")
//			    .postalCode("M5H 1J9")
//			    .country("Canada")
//			    .accessInstructions(
//			        "Keypad code - 54321. Enter through the west lobby."
//			    )
//			    .build();
		property1 = propertyRepository.save(property1);
		property2 = propertyRepository.save(property2);
		property3 = propertyRepository.save(property3);
//		property4 = propertyRepository.save(property4);
//		property5 = propertyRepository.save(property5);
		
		
		
		// Checklist Items
		// Property 1 checklist items
		ChecklistItem kitchen = generateChecklistItem(property1, "Clean kitchen counters and sink", 1, 1);
		ChecklistItem bathroom = generateChecklistItem(property1, "Clean and disinfect bathroom", 1, 3);
		ChecklistItem bedding = generateChecklistItem(property1, "Change bed linens", 1, 7);
		ChecklistItem supplies = generateChecklistItem(property1, "Check if supplies need to be replaced", 30, 4);
		ChecklistItem furnaceFilter = generateChecklistItem(property1, "Replace furnace filter", 45, 60);
		ChecklistItem ceilingCorners = generateChecklistItem(property1, "Dust ceiling corners", 15, 5);
		ChecklistItem baseboards = generateChecklistItem(property1, "Clean baseboards", 30, 35);
		checklistItemRepository
				.saveAll(List.of(kitchen, bathroom, bedding, supplies, furnaceFilter, ceilingCorners, baseboards));

		// Property 2 checklist items
		ChecklistItem kitchen2 = generateChecklistItem(property2, "Clean kitchen counters and sink", 1, 2);
		ChecklistItem bathroom2 = generateChecklistItem(property2, "Clean and disinfect bathroom", 1, 2);
		ChecklistItem bedding2 = generateChecklistItem(property2, "Change bed linens", 1, 6);
		ChecklistItem supplies2 = generateChecklistItem(property2, "Check if supplies need to be replaced", 30, 4);
		ChecklistItem hottub = generateChecklistItem(property2, "Check water level in hot tub", 7, 7);
		ChecklistItem windows2 = generateChecklistItem(property2, "Clean windows and balcony doors", 15, 3);
		ChecklistItem vents2 = generateChecklistItem(property2, "Dust vents and air returns", 45, 5);
		checklistItemRepository.saveAll(List.of(kitchen2, bathroom2, bedding2, supplies2, hottub, windows2, vents2));

		// Property 3 checklist items
		ChecklistItem floors3 = generateChecklistItem(property3, "Vacuum carpets and mop floors", 1, 1);
		ChecklistItem bathroom3 = generateChecklistItem(property3, "Clean and disinfect bathrooms", 1, 4);
		ChecklistItem bedding3 = generateChecklistItem(property3, "Change bed linens", 1, 5);
		ChecklistItem supplies3 = generateChecklistItem(property3, "Check if supplies need to be replaced", 30, 4);
		ChecklistItem ceilingFans3 = generateChecklistItem(property3, "Dust ceiling fans", 15, 6);
		ChecklistItem furnaceFilter3 = generateChecklistItem(property3, "Replace furnace filter", 45, 7);
		ChecklistItem windows3 = generateChecklistItem(property3, "Clean interior windows", 20, 3);
		ChecklistItem sinks3 = generateChecklistItem(property3, "Check under sinks for leaks", 15, 2);
		checklistItemRepository
				.saveAll(List.of(floors3, bathroom3, bedding3, supplies3, ceilingFans3, furnaceFilter3, windows3, sinks3));

		// Property 4 checklist items
//		ChecklistItem kitchen4 = generateChecklistItem(property4, "Clean kitchen surfaces", 1, 1);
//		ChecklistItem bathroom4 = generateChecklistItem(property4, "Clean bathroom fixtures", 1, 3);
//		ChecklistItem towels4 = generateChecklistItem(property4, "Replace towels and linens", 1, 4);
//		ChecklistItem appliances4 = generateChecklistItem(property4, "Wipe appliance exteriors", 7, 5);
//		ChecklistItem windows4 = generateChecklistItem(property4, "Clean windows and mirrors", 14, 6);
//		ChecklistItem balcony4 = generateChecklistItem(property4, "Sweep and wash balcony", 30, 7);
//		ChecklistItem smokeDetectors4 = generateChecklistItem(property4, "Test smoke detectors", 60, 2);

//		checklistItemRepository
//				.saveAll(List.of(kitchen4, bathroom4, towels4, appliances4, windows4, balcony4, smokeDetectors4));

		// Property 5 checklist items
//		ChecklistItem floors5 = generateChecklistItem(property5, "Sweep and mop floors", 1, 2);
//		ChecklistItem shower5 = generateChecklistItem(property5, "Clean glass shower doors", 1, 1);
//		ChecklistItem surfaces5 = generateChecklistItem(property5, "Wipe counters and surfaces", 1, 4);
//		ChecklistItem shelves5 = generateChecklistItem(property5, "Dust shelves and exposed pipes", 7, 6);
//		ChecklistItem rangeHood5 = generateChecklistItem(property5, "Clean range hood filter", 30, 7);
//		ChecklistItem sofa5 = generateChecklistItem(property5, "Vacuum sofa and cushions", 14, 5);
//		ChecklistItem alarms5 = generateChecklistItem(property5, "Test smoke and carbon monoxide alarms", 60, 3);
//		checklistItemRepository.saveAll(List.of(floors5, shower5, surfaces5, shelves5, rangeHood5, sofa5, alarms5));	
		

		
		// Cleanings
		LocalDateTime now = LocalDateTime.now();
		LocalDate today = now.toLocalDate();
		LocalDateTime scheduledStart = today.atTime(11, 0);
		LocalDateTime scheduledEnd = today.atTime(14, 0);
		LocalDateTime completedAt = scheduledEnd.isBefore(now) ? scheduledEnd : now;

		// Completed property 1 cleaning and its recorded checklist history
		Cleaning cleaning1 = Cleaning.builder()
		        .property(property1)
		        .cleaner(cleaner1)
		        .manager(manager1)
		        .dateTimeStart(scheduledStart)
		        .dateTimeEnd(scheduledEnd)
		        .dateTimeStarted(scheduledStart.isBefore(completedAt) ? scheduledStart : completedAt)
		        .dateTimeCompleted(completedAt)
		        .notes("Completed cleaning at Lakeshore Studio.")
		        .isComplete(true)
		        .build();
		for (ChecklistItem template : List.of(kitchen, bathroom, bedding, supplies, ceilingCorners)) {
			template.setLastCompleted(completedAt);
			cleaning1.getCleaningChecklistItems().add(CleaningChecklistItem.builder()
					.cleaning(cleaning1)
					.checklistItem(template)
					.isComplete(true)
					.build());
		}
		cleaningRepository.save(cleaning1);
		
		// property 2 cleaning
		Cleaning cleaning2 = Cleaning.builder()
		        .dateTimeStart(today.atTime(12, 0))
		        .dateTimeEnd(today.atTime(14, 0))
		        .isComplete(false)
		        .scheduleRecurringChecklist(true)
		        .build();
		cleaning2.setProperty(property2);
		cleaning2.setCleaner(cleaner2);
		cleaning2.setManager(manager1);
		cleaningRepository.save(cleaning2);
		
		// property 3 cleaning
		Cleaning cleaning3 = Cleaning.builder()
		        .dateTimeStart(today.atTime(12, 0))
		        .dateTimeEnd(today.atTime(15, 0))
		        .isComplete(false)
		        .scheduleRecurringChecklist(true)
		        .build();
		cleaning3.setProperty(property3);
		cleaning3.setCleaner(cleaner1);
		cleaning3.setManager(manager1);
		cleaningRepository.save(cleaning3);

		// Availabilities
		seedAvailability(cleaner1, DayOfWeek.values());
		seedAvailability(cleaner2, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY);

		// Generate five cleanings per property over the next three months.
		ThreadLocalRandom random = ThreadLocalRandom.current();
		long lastDayOffset = ChronoUnit.DAYS.between(today, today.plusMonths(3));

		List<Property> properties = List.of(property1, property2, property3);
		List<User> cleaners = List.of(cleaner1, cleaner2);
		Set<Integer> unassignedCleaningNumbers = Set.of(1, 4, 7, 10);

		for (int number = 1; number <= 5 * properties.size(); number++) {
			LocalDate cleaningDay = today.plusDays(random.nextLong(1, lastDayOffset + 1));

			// Seven half-hour starts from 11:00 AM through 2:00 PM.
			LocalDateTime start = cleaningDay.atTime(11, 0).plusMinutes(30L * random.nextInt(7));
			LocalDateTime end = start.plusHours(random.nextInt(2, 5));

			Cleaning cleaning = Cleaning.builder().dateTimeStart(start).dateTimeEnd(end)
					.notes("Automatically generated cleaning " + number)
					.isComplete(false)
					.scheduleRecurringChecklist(true)
					.build();

			cleaning.setProperty(properties.get((number - 1) % properties.size()));
			cleaning.setManager(manager1);

			if (!unassignedCleaningNumbers.contains(number)) {
				cleaning.setCleaner(cleaners.get(random.nextInt(cleaners.size())));
			}

			cleaningRepository.save(cleaning);
		}

		// These properties were inserted in this transaction and are locked until commit.
		for (Property property : properties) {
			recurringChecklistService.reconcile(property.getId());
		}

		// Generate 5 cleanings for today
//		LocalDate currentDate = LocalDate.now();
//		ThreadLocalRandom todayRandom = ThreadLocalRandom.current();
//
//		List<Property> todayProperties = List.of(property1, property2);
//		List<User> availableCleaners = List.of(cleaner1, cleaner2);
//
//		for (int i = 0; i < 5; i++) {
//		    // Duration is either 2 or 3 hours
//		    int durationHours = todayRandom.nextInt(2, 4);
//
//		    int earliestStartMinutes = 8 * 60;
//		    int latestStartMinutes = (24 - durationHours) * 60;
//
//		    // Number of available 30-minute start slots, including the last slot
//		    int slotCount =
//		            ((latestStartMinutes - earliestStartMinutes) / 30) + 1;
//
//		    int startMinutes =
//		            earliestStartMinutes + todayRandom.nextInt(slotCount) * 30;
//
//		    int hour = startMinutes / 60;
//		    int minute = startMinutes % 60;
//
//		    LocalDateTime start = currentDate.atTime(hour, minute);
//		    LocalDateTime end = start.plusHours(durationHours);
//
//		    Cleaning cleaning = Cleaning.builder()
//		            .dateTimeStart(start)
//		            .dateTimeEnd(end)
//		            .notes("Automatically generated cleaning for today " + (i + 1))
//		            .build();
//
//		    cleaning.setProperty(
//		            todayProperties.get(i % todayProperties.size())
//		    );
//		    cleaning.setManager(manager1);
//
//		    // Assign cleanings 1, 3, and 5; leave 2 and 4 unassigned
//		    if (i % 2 == 0) {
//		        cleaning.setCleaner(
//		                availableCleaners.get(
//		                        todayRandom.nextInt(availableCleaners.size())
//		                )
//		        );
//		    }
//
//		    cleaningRepository.save(cleaning);
//		}
	}
}
