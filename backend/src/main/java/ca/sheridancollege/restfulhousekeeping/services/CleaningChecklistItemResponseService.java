package ca.sheridancollege.restfulhousekeeping.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningChecklistItem;
import ca.sheridancollege.restfulhousekeeping.models.CleaningChecklistItemResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningChecklistItemRepository;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CleaningChecklistItemResponseService {

	private final CleaningChecklistItemRepository cleaningChecklistItemRepository;

	public List<CleaningChecklistItemResponse> getItemsForCleaning(
			Long cleaningId) {

		return cleaningChecklistItemRepository
				.findAllByCleaning_Id(cleaningId)
				.stream()
				.map(this::toResponse)
				.toList();
	}

	public CleaningChecklistItemResponse toResponse(CleaningChecklistItem item) {
		ChecklistItem checklistItem = item.getChecklistItem();

		return new CleaningChecklistItemResponse(
				item.getId(),
				checklistItem != null
						? checklistItem.getDescription()
						: item.getCustomDescription(),
				item.getIsComplete()
		);
	}

}
