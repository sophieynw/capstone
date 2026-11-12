package ca.sheridancollege.restfulhousekeeping.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import ca.sheridancollege.restfulhousekeeping.beans.Cleaning;
import ca.sheridancollege.restfulhousekeeping.beans.Issue;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.CreateIssueRequest;
import ca.sheridancollege.restfulhousekeeping.models.IssueResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.CleaningRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.IssueRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;


@RestController
@AllArgsConstructor
@RequestMapping("/issues")
@SecurityRequirement(name = "Bearer Authentication")
public class IssueController {

    private final IssueRepository issueRepository;
    private final CleaningRepository cleaningRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<IssueResponse> getForLoggedInManager(@AuthenticationPrincipal User manager) {
        return issueRepository
                .findByCleaning_Property_Manager_IdOrderByReportedAtDesc(manager.getId())
                .stream()
                .map(IssueResponse::fromIssue).toList();
    }

    // the reporter is whoever is logged in
    @PostMapping
    public IssueResponse create(@RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal User principal) {

        if (request.getCleaningId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "cleaningId is required");
        }
        validateText(request);

        Cleaning cleaning = cleaningRepository.findById(request.getCleaningId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cleaning not found"));
        User reporter = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));

        Issue issue = Issue.builder()
                .cleaning(cleaning)
                .reportedBy(reporter)
                .issueType(request.getIssueType().trim())
                .description(request.getDescription().trim())
                .reportedAt(LocalDateTime.now())
                .build();

        return IssueResponse.fromIssue(issueRepository.save(issue));
    }

    // only updates the type and description (cleaningId in the body is ignored)
    @PutMapping("/{id}")
    public ResponseEntity<IssueResponse> update(@PathVariable Long id,
            @RequestBody CreateIssueRequest request,
            @AuthenticationPrincipal User manager) {
        validateText(request);

        Issue existing = findOwnIssue(id, manager);
        existing.setIssueType(request.getIssueType().trim());
        existing.setDescription(request.getDescription().trim());
        return ResponseEntity.ok(IssueResponse.fromIssue(issueRepository.save(existing)));
    }

    // resolving an issue deletes it
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
            @AuthenticationPrincipal User manager) {
        Issue issue = findOwnIssue(id, manager);
        issueRepository.delete(issue);
        return ResponseEntity.noContent().build();
    }

    // finds an issue on one of the manager's own properties,
    // otherwise acts like it doesn't exist
    private Issue findOwnIssue(Long id, User manager) {
        return issueRepository.findById(id)
                .filter(issue -> issue.getCleaning() != null
                        && issue.getCleaning().getProperty() != null
                        && issue.getCleaning().getProperty().getManager() != null
                        && issue.getCleaning().getProperty().getManager().getId().equals(manager.getId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Issue not found"));
    }

    private void validateText(CreateIssueRequest request) {
        if (request.getIssueType() == null || request.getIssueType().isBlank()
                || request.getDescription() == null || request.getDescription().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "issueType and description are required");
        }
    }
}