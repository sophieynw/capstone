package ca.sheridancollege.restfulhousekeeping.repositories;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import ca.sheridancollege.restfulhousekeeping.beans.Conversation;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
	
	Optional<Conversation> findByManager_IdAndCleaner_Id(
	        Long managerId,
	        Long cleanerId
	    );

    List<Conversation> findByManager_IdOrCleaner_IdOrderByCreatedAtDesc(
        Long managerId,
        Long cleanerId
    );

}
