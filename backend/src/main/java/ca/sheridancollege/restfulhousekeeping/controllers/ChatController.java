package ca.sheridancollege.restfulhousekeeping.controllers;

import ca.sheridancollege.restfulhousekeeping.models.ChatMessageResponse;
import ca.sheridancollege.restfulhousekeeping.models.ConversationResponse;
import ca.sheridancollege.restfulhousekeeping.services.ChatService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations/with/{userId}")
    public ConversationResponse findOrCreateConversation(
            @PathVariable Long userId,
            Principal principal) {
        return chatService.findOrCreateConversation(userId, principal);
    }

    @PostMapping("/conversations/manager")
    public ConversationResponse findOrCreateManagerConversation(
            Principal principal) {
        return chatService.findOrCreateManagerConversation(principal);
    }

    @GetMapping("/conversations")
    public List<ConversationResponse> getConversations(
            Principal principal) {
        return chatService.getConversations(principal);
    }

    @GetMapping("/conversations/{conversationId}/messages")
    public Page<ChatMessageResponse> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size,
            Principal principal) {
        return chatService.getMessages(
            conversationId,
            page,
            size,
            principal
        );
    }
}
