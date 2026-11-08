package ca.sheridancollege.restfulhousekeeping.repositories;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ca.sheridancollege.restfulhousekeeping.beans.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
	
	Page<ChatMessage> findByConversation_IdOrderBySentAtDesc(
	        Long conversationId,
	        Pageable pageable
	    );

}
