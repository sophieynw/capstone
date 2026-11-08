package ca.sheridancollege.restfulhousekeeping.services;

import java.util.List;

import org.springframework.stereotype.Service;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.CleaningChecklistItem;
import ca.sheridancollege.restfulhousekeeping.repositories.ChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import jakarta.transaction.Transactional;
import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class CleaningService {

    private final CleaningRepository cleaningRepository;
    private final CleaningChecklistItemRepository cciRepository;
    private final ChecklistItemRepository checklistItemRepository;

    @Transactional
    public Cleaning createCleaning(
            Cleaning cleaning,
            List<Long> checklistItemIds) {

        cleaning.setId(null);
        cleaning.setIsComplete(false);

        Cleaning savedCleaning = cleaningRepository.save(cleaning);

        List<CleaningChecklistItem> cleaningItems =
                checklistItemRepository.findAllById(checklistItemIds)
                        .stream()
                        .map(checklistItem ->
                                CleaningChecklistItem.builder()
                                        .cleaning(savedCleaning)
                                        .checklistItem(checklistItem)
                                        .isComplete(false)
                                        .build()
                        )
                        .toList();

        cciRepository.saveAll(cleaningItems);
        savedCleaning.setCleaningChecklistItems(cleaningItems);

        return savedCleaning;
    }
}