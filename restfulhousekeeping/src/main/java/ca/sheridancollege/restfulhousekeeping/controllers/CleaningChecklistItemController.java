package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningChecklistItemResponse;
import ca.sheridancollege.restfulhousekeeping.models.CreateCleaningChecklistItemRequest;
import ca.sheridancollege.restfulhousekeeping.models.UpdateCleaningChecklistItemRequest;
import ca.sheridancollege.restfulhousekeeping.services.CleaningChecklistItemService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@AllArgsConstructor
@RequestMapping("/cleanings/{cleaningId}/checklist-items")
@SecurityRequirement(name = "Bearer Authentication")
public class CleaningChecklistItemController {

    private final CleaningChecklistItemService checklistItemService;

    @PostMapping
    public ResponseEntity<CleaningChecklistItemResponse> create(
            @PathVariable Long cleaningId,
            @Valid @RequestBody CreateCleaningChecklistItemRequest request,
            @AuthenticationPrincipal User manager) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                checklistItemService.addCustomItem(
                        cleaningId,
                        request.description(),
                        manager
                )
        );
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long cleaningId,
            @PathVariable Long itemId,
            @AuthenticationPrincipal User manager) {
        checklistItemService.deleteItem(cleaningId, itemId, manager);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{itemId}")
    public ResponseEntity<CleaningChecklistItemResponse> updateCompletion(
            @PathVariable Long cleaningId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateCleaningChecklistItemRequest request,
            @AuthenticationPrincipal User cleaner) {
        return ResponseEntity.ok(
                checklistItemService.updateCompletion(
                        cleaningId,
                        itemId,
                        request.isComplete(),
                        cleaner
                )
        );
    }
}
