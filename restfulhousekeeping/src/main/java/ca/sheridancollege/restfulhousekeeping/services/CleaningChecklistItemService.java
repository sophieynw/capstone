package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningChecklistItemResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@AllArgsConstructor
public class CleaningChecklistItemService {

    private final CleaningRepository cleaningRepository;
    private final CleaningChecklistItemRepository itemRepository;
    private final CleaningChecklistItemResponseService responseService;
    private final PropertyRepository propertyRepository;
    private final RecurringChecklistService recurringChecklistService;

    @Transactional
    public CleaningChecklistItemResponse addCustomItem(
            Long cleaningId,
            String description,
            User manager) {
        Cleaning cleaning = getManagedCleaning(cleaningId, manager);

        CleaningChecklistItem item = CleaningChecklistItem.builder()
                .cleaning(cleaning)
                .customDescription(description.trim())
                .isComplete(false)
                .build();

        return responseService.toResponse(itemRepository.save(item));
    }

    @Transactional
    public void deleteItem(Long cleaningId, Long itemId, User manager) {
        propertyRepository.findByCleaningIdForUpdate(cleaningId);
        Cleaning cleaning = getManagedCleaning(cleaningId, manager);

        CleaningChecklistItem item = itemRepository
                .findByIdAndCleaning_Id(itemId, cleaningId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cleaning checklist item not found"
                ));

        cleaning.getCleaningChecklistItems().removeIf(existing -> itemId.equals(existing.getId()));
        itemRepository.delete(item);
        if (item.getChecklistItem() != null) {
            cleaning.getExcludedChecklistItemIds().add(item.getChecklistItem().getId());
            recurringChecklistService.reconcile(cleaning.getProperty().getId());
        }
    }

    @Transactional
    public CleaningChecklistItemResponse updateCompletion(
        Long cleaningId,
        Long itemId,
        boolean isComplete,
        User user) {

        propertyRepository.findByCleaningIdForUpdate(cleaningId);
        Cleaning cleaning = cleaningRepository.findByIdForUpdate(cleaningId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cleaning not found"
            ));

        // Only the assigned cleaner may tick items
        if (user == null
            || user.getRole() != Role.CLEANER
            || cleaning.getCleaner() == null
            || !cleaning.getCleaner().getId().equals(user.getId())) {
        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Only the assigned cleaner can update this checklist"
        );
    }

    if (Boolean.TRUE.equals(cleaning.getIsComplete())) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "Completed cleaning checklist items cannot be changed"
        );
    }

    CleaningChecklistItem item = itemRepository
            .findByIdAndCleaning_Id(itemId, cleaningId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Cleaning checklist item not found"
            ));

    item.setIsComplete(isComplete);

    return responseService.toResponse(itemRepository.save(item));
}

    private Cleaning getManagedCleaning(Long cleaningId, User manager) {
        if (manager == null || manager.getRole() != Role.MANAGER) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only managers can edit cleaning checklist items"
            );
        }

        Cleaning cleaning = cleaningRepository.findByIdForUpdate(cleaningId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cleaning not found"
                ));

        if (!cleaning.getManager().getId().equals(manager.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This cleaning is not managed by the current manager"
            );
        }

        if (Boolean.TRUE.equals(cleaning.getIsComplete())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Completed cleaning checklist items cannot be changed"
            );
        }

        return cleaning;
    }
}
