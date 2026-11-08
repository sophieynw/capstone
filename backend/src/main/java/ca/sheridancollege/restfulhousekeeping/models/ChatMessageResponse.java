package ca.sheridancollege.restfulhousekeeping.models;

import java.time.LocalDateTime;

import ca.sheridancollege.restfulhousekeeping.beans.ChatMessage;

public record ChatMessageResponse(
    Long id,
    Long conversationId,
    Long senderId,
    String senderName,
    String body,
    LocalDateTime sentAt,
    LocalDateTime readAt
) {
    public static ChatMessageResponse fromMessage(ChatMessage message) {
        return new ChatMessageResponse(
            message.getId(),
            message.getConversation().getId(),
            message.getSender().getId(),
            message.getSender().getFirstName()
                + " "
                + message.getSender().getLastName(),
            message.getBody(),
            message.getSentAt(),
            message.getReadAt()
        );
    }
}
