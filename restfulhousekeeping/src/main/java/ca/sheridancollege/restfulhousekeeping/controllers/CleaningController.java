package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningSource;
import ca.sheridancollege.restfulhousekeeping.beans.Property;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningResponse;
import ca.sheridancollege.restfulhousekeeping.models.UpdateCleaningCleanerRequest;
import ca.sheridancollege.restfulhousekeeping.models.UpdateCleaningScheduleRequest;
import ca.sheridancollege.restfulhousekeeping.repositories.ChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import ca.sheridancollege.restfulhousekeeping.services.CleaningPhotoService;
import ca.sheridancollege.restfulhousekeeping.services.CleaningResponseService;
import ca.sheridancollege.restfulhousekeeping.services.RecurringChecklistService;
import jakarta.validation.Valid;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

@RestController
@AllArgsConstructor
@RequestMapping("/cleanings")
@SecurityRequirement(name = "Bearer Authentication")
public class CleaningController {

	private final CleaningRepository cleaningRepository;
	private final CleaningResponseService cleaningResponseService;
	private final CleaningPhotoService cleaningPhotoService;
	private final ChecklistItemRepository checklistItemRepository;
	private final PropertyRepository propertyRepository;
	private final RecurringChecklistService recurringChecklistService;

	// GET all upcoming cleanings by userId
	@GetMapping("/upcoming/{userId}")
	public List<CleaningResponse> getMyUpcomingCleanings(@PathVariable Long userId) {
		return cleaningResponseService.getMyUpcomingCleanings(userId);
	}

	// GET all completed cleanings by cleaner ID
	@GetMapping("/completed/{userId}")
	public List<CleaningResponse> getCompletedCleanings(@PathVariable Long userId) {
    	return cleaningRepository.findByCleanerIdAndIsCompleteTrue(userId)
            .stream()
            .map(cleaningResponseService::toCleaningResponse)
            .toList();
	}	

	// GET first cleaning by property ID
	@GetMapping("/upcoming/{propertyId}/first")
	public ResponseEntity<CleaningResponse> getNextCleaningByPropertyId(@PathVariable Long propertyId) {
		return cleaningResponseService.getFirstCleaningByProperty(propertyId).map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.noContent().build());
	}

	@GetMapping("/{id}")
	public ResponseEntity<CleaningResponse> getById(@PathVariable Long id) {
		return cleaningRepository.findById(id)
				.map(cleaningResponseService::toCleaningResponse)
				.map(ResponseEntity::ok)
				.orElse(ResponseEntity.notFound().build());
	}

	// CREATE new cleaning record
	@PostMapping
	@Transactional
	public ResponseEntity<CleaningResponse> create(
			@RequestBody Cleaning cleaning, @AuthenticationPrincipal User manager) {

		if (cleaning.getProperty() == null || cleaning.getProperty().getId() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Property ID is required");
		}
		Property property = propertyRepository.findByIdForUpdate(cleaning.getProperty().getId())
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found"));
		if (manager == null || manager.getRole() != Role.MANAGER
				|| !property.getManager().getId().equals(manager.getId())
				|| cleaning.getManager() == null || !manager.getId().equals(cleaning.getManager().getId())) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You can only schedule cleanings for your own properties");
		}
		cleaning.setProperty(property);
		cleaning.setManager(property.getManager());

		if (cleaning.getCleaner() != null) {
			if (cleaning.getCleaner().getId() == null) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cleaner ID is required");
			}
			cleaning.setCleaner(cleaningResponseService.resolveAvailableCleaner(
					cleaning.getCleaner().getId(), manager, cleaning.getDateTimeStart(), cleaning.getDateTimeEnd()));
		}

		cleaning.setId(null);
		cleaning.setIsComplete(false);
		cleaning.setSource(CleaningSource.MANUAL);
		cleaning.setDateTimeStarted(null);
		cleaning.setDateTimeCompleted(null);
		cleaning.setScheduleRecurringChecklist(true);
		if (cleaning.getExcludedChecklistItemIds() == null) {
			cleaning.setExcludedChecklistItemIds(new HashSet<>());
		}
		if (cleaning.getCleaningChecklistItems() == null) {
			cleaning.setCleaningChecklistItems(new ArrayList<>());
		}

		cleaning.getCleaningChecklistItems().forEach(item -> {
			item.setId(null);
			item.setCleaning(cleaning);
			item.setIsComplete(false);

			if (item.getChecklistItem() != null) {
				Long checklistItemId = item.getChecklistItem().getId();

				ChecklistItem existingChecklistItem = checklistItemRepository.findById(checklistItemId)
						.filter(template -> property.getId().equals(template.getProperty().getId()))
						.orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
								"Checklist item does not belong to this property"));

				item.setChecklistItem(existingChecklistItem);
			}
		});

		Cleaning savedCleaning = cleaningRepository.save(cleaning);
		recurringChecklistService.reconcile(property.getId());
		CleaningResponse cleaningResponse = cleaningResponseService.toCleaningResponse(savedCleaning);

		return ResponseEntity.status(HttpStatus.CREATED).body(cleaningResponse);
	}

	// Marks cleaning a complete
	@PatchMapping("/{cleaningId}/complete")
	public ResponseEntity<CleaningResponse> completeCleaning(@PathVariable Long cleaningId) {
		CleaningResponse response = cleaningResponseService.completeCleaning(cleaningId);
		return ResponseEntity.ok(response);
	}

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, String>> handleResponseStatusException(ResponseStatusException exception) {
		return ResponseEntity.status(exception.getStatusCode())
				.body(Map.of("message", exception.getReason() == null
						? "The cleaning request could not be completed."
						: exception.getReason()));
	}

	@PatchMapping("/{cleaningId}/cleaner")
	public ResponseEntity<CleaningResponse> updateCleaner(
			@PathVariable Long cleaningId,
			@Valid @RequestBody UpdateCleaningCleanerRequest request,
			@AuthenticationPrincipal User manager) {
		return ResponseEntity.ok(cleaningResponseService.updateCleaner(
				cleaningId,
				request.cleanerId(),
				manager
		));
	}

	@PatchMapping("/{cleaningId}/schedule")
	public ResponseEntity<CleaningResponse> updateSchedule(
			@PathVariable Long cleaningId,
			@Valid @RequestBody UpdateCleaningScheduleRequest request,
			@AuthenticationPrincipal User manager) {
		return ResponseEntity.ok(cleaningResponseService.updateSchedule(
				cleaningId,
				request.dateTimeStart(),
				request.dateTimeEnd(),
				manager
		));
	}

	@PutMapping("/{id}")
	@Transactional
	public ResponseEntity<Cleaning> update(
			@PathVariable Long id,
			@RequestBody Cleaning updated,
			@AuthenticationPrincipal User manager) {
		propertyRepository.findByCleaningIdForUpdate(id);
		return cleaningRepository.findByIdForUpdate(id).map(existing -> {
			if (manager == null
					|| manager.getRole() != Role.MANAGER
					|| !existing.getManager().getId().equals(manager.getId())) {
				throw new ResponseStatusException(HttpStatus.FORBIDDEN);
			}
			if (Boolean.TRUE.equals(existing.getIsComplete())) {
				throw new ResponseStatusException(
						HttpStatus.CONFLICT,
						"A completed cleaning cannot be updated"
				);
			}
			existing.setDateTimeStart(updated.getDateTimeStart());
			existing.setDateTimeEnd(updated.getDateTimeEnd());
			existing.setNotes(updated.getNotes());
			if (Boolean.TRUE.equals(updated.getIsComplete())) {
				recurringChecklistService.recordCompletion(existing);
			} else {
				existing.setIsComplete(updated.getIsComplete());
				recurringChecklistService.reconcile(existing.getProperty().getId());
			}
			return ResponseEntity.ok(cleaningRepository.save(existing));
		}).orElse(ResponseEntity.notFound().build());
	}

	@DeleteMapping("/{id}")
	@Transactional
	public ResponseEntity<Void> delete(@PathVariable Long id, @AuthenticationPrincipal User manager) {
		propertyRepository.findByCleaningIdForUpdate(id);
		Cleaning cleaning = cleaningRepository.findByIdForUpdate(id)
				.filter(existing -> existing.getManager().getId().equals(manager.getId()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

		if (Boolean.TRUE.equals(cleaning.getIsComplete())) {
			throw new ResponseStatusException(
					HttpStatus.CONFLICT,
					"A completed cleaning cannot be deleted"
			);
		}

		cleaningPhotoService.deleteCloudPhotosAfterCleaningRemoval(List.of(id));
		cleaningRepository.delete(cleaning);
		recurringChecklistService.reconcile(cleaning.getProperty().getId());
		return ResponseEntity.noContent().build();
	}

}
