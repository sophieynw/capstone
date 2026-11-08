package ca.sheridancollege.restfulhousekeeping.models;

import java.time.LocalDateTime;

import ca.sheridancollege.restfulhousekeeping.beans.Conversation;
import ca.sheridancollege.restfulhousekeeping.beans.User;

public record ConversationResponse(
    Long id,
    Long otherUserId,
    String otherUserName,
    LocalDateTime createdAt
) {
    public static ConversationResponse fromConversation(
            Conversation conversation,
            User currentUser) {

        User otherUser =
            conversation.getManager().getId().equals(currentUser.getId())
                ? conversation.getCleaner()
                : conversation.getManager();

        return new ConversationResponse(
            conversation.getId(),
            otherUser.getId(),
            otherUser.getFirstName() + " " + otherUser.getLastName(),
            conversation.getCreatedAt()
        );
    }
}
