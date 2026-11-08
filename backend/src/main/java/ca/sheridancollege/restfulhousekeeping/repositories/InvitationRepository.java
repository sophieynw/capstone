package ca.sheridancollege.restfulhousekeeping.repositories;

import ca.sheridancollege.restfulhousekeeping.beans.Invitation;
import ca.sheridancollege.restfulhousekeeping.beans.InvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InvitationRepository extends JpaRepository<Invitation, Long> {

    List<Invitation> findByCleanerIdAndStatus(
        Long cleanerId,
        InvitationStatus status
    );

    List<Invitation> findByOrganizationIdAndStatus(
        Long organizationId,
        InvitationStatus status
    );

    List<Invitation> findByOrganizationId(Long organizationId);
    List<Invitation> findByCleanerId(Long cleanerId);

    Optional<Invitation> findByInvitedEmailIgnoreCaseAndOrganizationId(
        String invitedEmail,
        Long organizationId
    );

    List<Invitation> findByInvitedEmailIgnoreCaseAndStatus(
        String invitedEmail,
        InvitationStatus status
    );
}
