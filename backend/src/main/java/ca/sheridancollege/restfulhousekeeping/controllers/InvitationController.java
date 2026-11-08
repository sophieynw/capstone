package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.beans.InvitationStatus;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.InvitationRequest;
import ca.sheridancollege.restfulhousekeeping.models.InvitationResponse;
import ca.sheridancollege.restfulhousekeeping.services.InvitationService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("/invitations")
@SecurityRequirement(name = "Bearer Authentication")
public class InvitationController {

    private final InvitationService invitationService;

    @PostMapping
    public ResponseEntity<InvitationResponse> createInvitation(
            @AuthenticationPrincipal User manager,
            @Valid @RequestBody InvitationRequest request) {

        InvitationResponse response =
                invitationService.createInvitation(
                        request.email(),
                        manager
                );

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{invitationId}")
    public ResponseEntity<InvitationResponse> getInvitationById(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(
                invitationService.getInvitationById(invitationId, user)
        );
    }

    @GetMapping("/cleaner/{cleanerId}")
    public ResponseEntity<List<InvitationResponse>> getCleanerInvitations(
            @PathVariable Long cleanerId,
            @RequestParam(required = false) InvitationStatus status,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(
                invitationService.getCleanerInvitations(
                        cleanerId, status, user
                )
        );
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<InvitationResponse>> getOrganizationInvitations(
            @PathVariable Long organizationId,
            @RequestParam(required = false) InvitationStatus status,
            @AuthenticationPrincipal User user) {

        return ResponseEntity.ok(
                invitationService.getOrganizationInvitations(
                        organizationId, status, user
                )
        );
    }

    @PatchMapping("/{invitationId}/accept")
    public ResponseEntity<InvitationResponse> acceptInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal User cleaner) {

        return ResponseEntity.ok(
                invitationService.acceptInvitation(invitationId, cleaner)
        );
    }

    @PatchMapping("/{invitationId}/decline")
    public ResponseEntity<InvitationResponse> declineInvitation(
            @PathVariable Long invitationId,
            @AuthenticationPrincipal User cleaner) {

        return ResponseEntity.ok(
                invitationService.declineInvitation(invitationId, cleaner)
        );
    }
}
