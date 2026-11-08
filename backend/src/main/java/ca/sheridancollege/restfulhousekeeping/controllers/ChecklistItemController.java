package ca.sheridancollege.restfulhousekeeping.controllers;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ca.sheridancollege.restfulhousekeeping.beans.ChecklistItem;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.repositories.ChecklistItemRepository;
import ca.sheridancollege.restfulhousekeeping.services.RecurringChecklistService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;

@RestController
@RequestMapping("/checklist-items")
@AllArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ChecklistItemController {

    private final ChecklistItemRepository checklistItemRepository;
    private final RecurringChecklistService recurringChecklistService;

    @GetMapping("/{propertyId}/due")
    public List<ChecklistItem> getDueChecklistItems(
            @PathVariable Long propertyId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime start,
            @AuthenticationPrincipal User manager) {
        return recurringChecklistService.preview(propertyId, start, manager);
    }
    
    @GetMapping("/{propertyId}")
    public ResponseEntity<List<ChecklistItem>> getChecklistItems(@PathVariable Long propertyId) {
    	List<ChecklistItem> items = checklistItemRepository.findAllByProperty_Id(propertyId);
    	return ResponseEntity.ok(items);
    }
    


//    @GetMapping("/{id}")
//    public ResponseEntity<ChecklistItem> getById(@PathVariable Long id) {
//        return checklistItemRepository.findById(id)
//                .map(ResponseEntity::ok)
//                .orElse(ResponseEntity.notFound().build());
//    }
//
//    @PostMapping
//    public ChecklistItem create(@RequestBody ChecklistItem checklistItem) {
//        return checklistItemRepository.save(checklistItem);
//    }
//
//    @PutMapping("/{id}")
//    public ResponseEntity<ChecklistItem> update(@PathVariable Long id, @RequestBody ChecklistItem updated) {
//        return checklistItemRepository.findById(id).map(existing -> {
//            existing.setDescription(updated.getDescription());
//            existing.setFrequencyDays(updated.getFrequencyDays());
//            return ResponseEntity.ok(checklistItemRepository.save(existing));
//        }).orElse(ResponseEntity.notFound().build());
//    }
//
//    @DeleteMapping("/{id}")
//    public ResponseEntity<Void> delete(@PathVariable Long id) {
//        if (!checklistItemRepository.existsById(id)) {
//            return ResponseEntity.notFound().build();
//        }
//        checklistItemRepository.deleteById(id);
//        return ResponseEntity.noContent().build();
//    }
}
