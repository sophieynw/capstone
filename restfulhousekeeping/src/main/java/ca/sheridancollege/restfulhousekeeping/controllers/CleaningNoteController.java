package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CleaningNoteResponse;
import ca.sheridancollege.restfulhousekeeping.models.CreateCleaningNoteRequest;
import ca.sheridancollege.restfulhousekeeping.services.CleaningNoteService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/cleanings/{cleaningId}/notes")
@SecurityRequirement(name = "Bearer Authentication")
public class CleaningNoteController {

    private final CleaningNoteService noteService;

    @GetMapping
    public List<CleaningNoteResponse> list(
            @PathVariable Long cleaningId,
            @AuthenticationPrincipal User user) {
        return noteService.list(cleaningId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CleaningNoteResponse create(
            @PathVariable Long cleaningId,
            @Valid @RequestBody CreateCleaningNoteRequest request,
            @AuthenticationPrincipal User user) {
        return noteService.create(cleaningId, request.content(), user);
    }
}
