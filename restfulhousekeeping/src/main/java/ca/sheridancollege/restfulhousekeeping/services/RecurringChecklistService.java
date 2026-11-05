package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.repositories.ChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.PropertyRepository;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class RecurringChecklistService {
    private final ChecklistItemRepository checklistItemRepository;
    private final CleaningRepository cleaningRepository;
    private final PropertyRepository propertyRepository;

    @Transactional(readOnly = true)
    public List<ChecklistItem> preview(Long propertyId, LocalDateTime start, User manager) {
        if (manager == null || manager.getRole() != Role.MANAGER
                || propertyRepository.findByIdAndManager_Id(propertyId, manager.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Property not found");
        }
        return plan(propertyId, start, false);
    }

    // Call within the transaction holding the property's write lock.
    public void reconcile(Long propertyId) {
        plan(propertyId, null, true);
    }

    private List<ChecklistItem> plan(Long propertyId, LocalDateTime previewStart, boolean persist) {
        List<ChecklistItem> templates = checklistItemRepository.findAllByProperty_Id(propertyId);
        if (templates.isEmpty()) return List.of();

        Map<Long, LocalDate> projectedCompletions = new HashMap<>();
        for (Cleaning cleaning : cleaningRepository.findPendingChecklistSchedule(propertyId, LocalDateTime.now())) {
            if (previewStart != null && cleaning.getDateTimeStart().isAfter(previewStart)) break;

            LocalDate day = cleaning.getDateTimeStart().toLocalDate();
            Map<Long, CleaningChecklistItem> linkedItems = new HashMap<>();
            for (CleaningChecklistItem item : cleaning.getCleaningChecklistItems()) {
                if (item.getChecklistItem() != null) linkedItems.put(item.getChecklistItem().getId(), item);
            }

            for (ChecklistItem template : templates) {
                Long id = template.getId();
                CleaningChecklistItem item = linkedItems.get(id);
                boolean pinned = item != null && (!Boolean.TRUE.equals(item.getAutomaticallyScheduled())
                        || Boolean.TRUE.equals(item.getIsComplete()));
                boolean scheduled = Boolean.TRUE.equals(cleaning.getScheduleRecurringChecklist())
                        && !cleaning.getExcludedChecklistItemIds().contains(id)
                        && isDue(template, day, projectedCompletions.get(id));

                if (pinned || scheduled) {
                    projectedCompletions.put(id, day);
                    if (persist && item == null) {
                        cleaning.getCleaningChecklistItems().add(CleaningChecklistItem.builder()
                                .cleaning(cleaning)
                                .checklistItem(template)
                                .automaticallyScheduled(true)
                                .build());
                    }
                } else if (persist && item != null) {
                    cleaning.getCleaningChecklistItems().removeIf(existing -> existing == item);
                }
            }
        }

        if (previewStart == null) return List.of();
        return templates.stream()
                .filter(template -> isDue(template, previewStart.toLocalDate(), projectedCompletions.get(template.getId())))
                .toList();
    }

    private boolean isDue(ChecklistItem template, LocalDate day, LocalDate projectedCompletion) {
        LocalDate lastCompleted = template.getLastCompleted() == null
                ? null : template.getLastCompleted().toLocalDate();
        if (lastCompleted == null || (projectedCompletion != null && projectedCompletion.isAfter(lastCompleted))) {
            lastCompleted = projectedCompletion;
        }
        int frequency = template.getFrequencyDays() == null ? 0 : template.getFrequencyDays();
        return lastCompleted == null || !day.isBefore(lastCompleted.plusDays(Math.max(0, frequency)));
    }

    public void recordCompletion(Cleaning cleaning) {
        boolean newlyCompleted = !Boolean.TRUE.equals(cleaning.getIsComplete());
        cleaning.markComplete();
        if (!newlyCompleted) return;

        LocalDateTime completedAt = cleaning.getDateTimeCompleted();
        for (CleaningChecklistItem item : cleaning.getCleaningChecklistItems()) {
            ChecklistItem template = item.getChecklistItem();
            if (template != null && (template.getLastCompleted() == null
                    || template.getLastCompleted().isBefore(completedAt))) {
                template.setLastCompleted(completedAt);
            }
        }
        reconcile(cleaning.getProperty().getId());
    }
}
