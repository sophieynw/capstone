package ca.sheridancollege.restfulhousekeeping.controllers;

import java.security.Principal;

import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import ca.sheridancollege.restfulhousekeeping.models.ChatMessageResponse;
import ca.sheridancollege.restfulhousekeeping.models.SendChatMessageRequest;
import ca.sheridancollege.restfulhousekeeping.services.ChatService;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class ChatSocketController {

    private final ChatService chatService;

    @MessageMapping("/chat.send")
    @SendToUser(value = "/queue/messages", broadcast = false)
    public ChatMessageResponse sendMessage(
            SendChatMessageRequest request,
            Principal principal) {
        return chatService.saveAndBroadcastMessage(request, principal);
    }
}
