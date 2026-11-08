package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.*;
import ca.sheridancollege.restfulhousekeeping.models.InvitationResponse;
import ca.sheridancollege.restfulhousekeeping.repositories.InvitationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;

import jakarta.transaction.Transactional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class InvitationService {

    private final InvitationRepository invitationRepository;
    private final UserRepository userRepository;
    private final InvitationNotificationService notificationService;

    public InvitationService(
            InvitationRepository invitationRepository,
            UserRepository userRepository,
            InvitationNotificationService notificationService) {

        this.invitationRepository = invitationRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    @Transactional
    public InvitationResponse createInvitation(
            String cleanerEmail,
            User manager) {

        if (manager == null
                || manager.getRole() != Role.MANAGER
                || manager.getOrganization() == null) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only managers associated with an organization can invite cleaners");
        }

        String normalizedEmail = EmailPolicy.normalizeAndValidate(cleanerEmail);
        User cleaner = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);

        Organization organization = manager.getOrganization();

        if (cleaner != null && cleaner.getRole() != Role.CLEANER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only cleaners can be invited");
        }

        if (cleaner != null && cleaner.getOrganization() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This cleaner is already part of an organization");
        }

        Invitation existingInvitation = invitationRepository
                .findByInvitedEmailIgnoreCaseAndOrganizationId(
                        normalizedEmail,
                        organization.getId())
                .orElse(null);

        if (existingInvitation != null
                && existingInvitation.getStatus() == InvitationStatus.PENDING) {
            notificationService.invitationSent(
                    normalizedEmail, cleaner, manager, organization);
            return toResponse(existingInvitation);
        }

        Invitation invitation = existingInvitation == null
                ? new Invitation()
                : existingInvitation;
        invitation.setCleaner(cleaner);
        invitation.setInvitedEmail(normalizedEmail);
        invitation.setOrganization(organization);
        invitation.setInvitedBy(manager);
        invitation.setStatus(InvitationStatus.PENDING);

        LocalDateTime now = LocalDateTime.now();
        invitation.setCreatedAt(now);

        Invitation saved = invitationRepository.save(invitation);

        notificationService.invitationSent(
                normalizedEmail, cleaner, manager, organization);

        return toResponse(saved);
    }

    @Transactional
    public void reconcilePendingInvitations(User user) {
        List<Invitation> invitations = invitationRepository
                .findByInvitedEmailIgnoreCaseAndStatus(
                        user.getEmail(), InvitationStatus.PENDING);
        for (Invitation invitation : invitations) {
            if (user.getRole() == Role.CLEANER
                    && user.getOrganization() == null) {
                invitation.setCleaner(user);
            } else {
                invitation.setStatus(InvitationStatus.DECLINED);
                notificationService.invitationResponded(
                        invitation, user, InvitationStatus.DECLINED);
            }
        }

        if (!invitations.isEmpty()) {
            invitationRepository.saveAll(invitations);
        }
    }

    @Transactional
    public InvitationResponse getInvitationById(
            Long invitationId,
            User user) {

        Invitation invitation = findInvitation(invitationId);
        authorizeInvitationAccess(invitation, user);

        return toResponse(invitation);
    }

    @Transactional
    public List<InvitationResponse> getCleanerInvitations(
            Long cleanerId,
            InvitationStatus status,
            User user) {

        if (user.getRole() != Role.CLEANER
                || !user.getId().equals(cleanerId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You can only view your own invitations");
        }

        List<Invitation> invitations =
                status == null
                        ? invitationRepository.findByCleanerId(cleanerId)
                        : invitationRepository.findByCleanerIdAndStatus(
                                cleanerId, status);

        return invitations.stream()
                .map(this::toResponse)
                .toList();
    }

    // Retrieve invitations belonging to an organization.
    @Transactional
    public List<InvitationResponse> getOrganizationInvitations(
            Long organizationId,
            InvitationStatus status,
            User manager) {

        if (manager.getRole() != Role.MANAGER
                || manager.getOrganization() == null
                || !manager.getOrganization().getId()
                        .equals(organizationId)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot view invitations for this organization");
        }

        List<Invitation> invitations =
                status == null
                        ? invitationRepository.findByOrganizationId(
                                organizationId)
                        : invitationRepository.findByOrganizationIdAndStatus(
                                organizationId, status);

        return invitations.stream()
                .map(this::toResponse)
                .toList();
    }

    // Accept an invitation.
    @Transactional
    public InvitationResponse acceptInvitation(
            Long invitationId,
            User cleaner) {

        Invitation invitation = findInvitation(invitationId);

        validateInvitedCleaner(invitation, cleaner);
        validatePendingInvitation(invitation);

        User managedCleaner = userRepository.findById(cleaner.getId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Cleaner not found"));

        Organization organization = invitation.getOrganization();

        if (managedCleaner.getOrganization() != null) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cleaner already belongs to an organization");
        }

        managedCleaner.setOrganization(organization);
        userRepository.save(managedCleaner);

        invitation.setStatus(InvitationStatus.ACCEPTED);
        Invitation savedInvite = invitationRepository.save(invitation);

        notificationService.invitationResponded(
                savedInvite, managedCleaner, InvitationStatus.ACCEPTED);
        declineOtherPendingInvitations(managedCleaner.getId(), savedInvite.getId(), managedCleaner);
        return toResponse(savedInvite);
    }

    private void declineOtherPendingInvitations(
            Long cleanerId,
            Long respondedId,
            User cleaner) {

        List<Invitation> others = invitationRepository
                .findByCleanerIdAndStatus(cleanerId, InvitationStatus.PENDING)
                .stream()
                .filter(invitation -> !invitation.getId().equals(respondedId))
                .toList();

        others.forEach(invitation -> invitation.setStatus(InvitationStatus.DECLINED));
        invitationRepository.saveAll(others);
        others.forEach(invitation -> notificationService.invitationResponded(
                invitation, cleaner, InvitationStatus.DECLINED));
    }

    // Decline an invitation.
    @Transactional
    public InvitationResponse declineInvitation(
            Long invitationId,
            User cleaner) {

        Invitation invitation = findInvitation(invitationId);

        validateInvitedCleaner(invitation, cleaner);
        validatePendingInvitation(invitation);

        invitation.setStatus(InvitationStatus.DECLINED);
        Invitation savedInvite = invitationRepository.save(invitation);
        notificationService.invitationResponded(
                savedInvite, cleaner, InvitationStatus.DECLINED);
        declineOtherPendingInvitations(cleaner.getId(), savedInvite.getId(), cleaner);
        return toResponse(savedInvite);
    }

    private Invitation findInvitation(Long invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Invitation not found"));
    }

    private void authorizeInvitationAccess(
            Invitation invitation,
            User user) {

        boolean isInvitedCleaner =
                user.getRole() == Role.CLEANER
                && invitation.getCleaner() != null
                && invitation.getCleaner().getId().equals(user.getId());

        boolean isOrganizationManager =
                user.getRole() == Role.MANAGER
                && user.getOrganization() != null
                && user.getOrganization().getId()
                        .equals(invitation.getOrganization().getId());

        if (!isInvitedCleaner && !isOrganizationManager) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "You cannot access this invitation");
        }
    }

    private void validateInvitedCleaner(
            Invitation invitation,
            User cleaner) {

        if (cleaner.getRole() != Role.CLEANER
                || invitation.getCleaner() == null
                || !invitation.getCleaner().getId()
                        .equals(cleaner.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "This invitation was not issued to you");
        }
    }

    private void validatePendingInvitation(Invitation invitation) {
        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Invitation is no longer pending");
        }

    }

    private InvitationResponse toResponse(Invitation invitation) {
        User cleaner = invitation.getCleaner();
        Organization organization = invitation.getOrganization();
        User manager = invitation.getInvitedBy();

        return new InvitationResponse(
                invitation.getId(),
                cleaner == null ? null : cleaner.getId(),
                cleaner == null
                        ? null
                        : cleaner.getFirstName() + " " + cleaner.getLastName(),
                invitation.getInvitedEmail(),
                organization.getId(),
                organization.getName(),
                manager.getId(),
                invitation.getStatus(),
                invitation.getCreatedAt()
        );
    }
}
