package ca.sheridancollege.restfulhousekeeping.services;

import ca.sheridancollege.restfulhousekeeping.beans.ChatMessage;
import ca.sheridancollege.restfulhousekeeping.beans.Conversation;
import ca.sheridancollege.restfulhousekeeping.beans.Role;
import ca.sheridancollege.restfulhousekeeping.beans.User;
import ca.sheridancollege.restfulhousekeeping.models.ChatMessageResponse;
import ca.sheridancollege.restfulhousekeeping.models.ConversationResponse;
import ca.sheridancollege.restfulhousekeeping.models.SendChatMessageRequest;
import ca.sheridancollege.restfulhousekeeping.repositories.ChatMessageRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.ConversationRepository;
import ca.sheridancollege.restfulhousekeeping.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;
    private static final int MAX_PAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Loads the authenticated user.
     *
     * Principal.getName() contains the username established by Spring Security.
     */
    @Transactional(readOnly = true)
    public User getCurrentUser(Principal principal) {
        if (principal == null || principal.getName() == null) {
            throw new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "You must be signed in"
            );
        }

        return userRepository.findByUsernameIgnoreCase(principal.getName())
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "Authenticated user could not be found"
            ));
    }

    /**
     * Finds an existing manager-cleaner conversation or creates one.
     */
    @Transactional
    public ConversationResponse findOrCreateConversation(
            Long otherUserId,
            Principal principal) {

        if (otherUserId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A user ID is required"
            );
        }

        User currentUser = getCurrentUser(principal);

        User otherUser = userRepository.findById(otherUserId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "The selected user could not be found"
            ));

        if (currentUser.getId().equals(otherUser.getId())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "You cannot start a conversation with yourself"
            );
        }

        validateSameOrganization(currentUser, otherUser);

        User manager;
        User cleaner;

        if (currentUser.getRole() == Role.MANAGER
                && otherUser.getRole() == Role.CLEANER) {
            manager = currentUser;
            cleaner = otherUser;
        } else if (currentUser.getRole() == Role.CLEANER
                && otherUser.getRole() == Role.MANAGER) {
            manager = otherUser;
            cleaner = currentUser;
        } else {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A conversation must be between a manager and a cleaner"
            );
        }

        Conversation conversation = conversationRepository
            .findByManager_IdAndCleaner_Id(
                manager.getId(),
                cleaner.getId()
            )
            .orElseGet(() -> conversationRepository.save(
                Conversation.builder()
                    .organization(manager.getOrganization())
                    .manager(manager)
                    .cleaner(cleaner)
                    .build()
            ));

        return ConversationResponse.fromConversation(
            conversation,
            currentUser
        );
    }

    /**
     * Find or create conversation with manager.
     */
    @Transactional
    public ConversationResponse findOrCreateManagerConversation(
            Principal principal) {

        User cleaner = getCurrentUser(principal);

        if (cleaner.getRole() != Role.CLEANER) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only cleaners can use this endpoint"
            );
        }

        if (cleaner.getOrganization() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "The cleaner does not belong to an organization"
            );
        }

        User manager = userRepository
                .findFirstByOrganization_IdAndRoleOrderByIdAsc(
                        cleaner.getOrganization().getId(),
                        Role.MANAGER
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "No manager belongs to this organization"
                ));

        return findOrCreateConversation(manager.getId(), principal);
    }

    /**
     * Returns every conversation belonging to the authenticated user.
     */
    @Transactional(readOnly = true)
    public List<ConversationResponse> getConversations(
            Principal principal) {

        User currentUser = getCurrentUser(principal);
        Long currentUserId = currentUser.getId();

        return conversationRepository
            .findByManager_IdOrCleaner_IdOrderByCreatedAtDesc(
                currentUserId,
                currentUserId
            )
            .stream()
            .map(conversation ->
                ConversationResponse.fromConversation(
                    conversation,
                    currentUser
                )
            )
            .toList();
    }

    /**
     * Returns a page of messages from a conversation.
     *
     * The repository returns the newest messages first.
     */
    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getMessages(
            Long conversationId,
            int page,
            int size,
            Principal principal) {

        User currentUser = getCurrentUser(principal);

        Conversation conversation = getConversation(conversationId);

        verifyMembership(conversation, currentUser);

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        return chatMessageRepository
            .findByConversation_IdOrderBySentAtDesc(
                conversation.getId(),
                PageRequest.of(safePage, safeSize)
            )
            .map(ChatMessageResponse::fromMessage);
    }

    /**
     * Validates, saves, and broadcasts a new chat message.
     *
     * The sender is determined from the authenticated Principal.
     * After the message is saved, the recipient receives the
     * server-generated ChatMessageResponse. The socket controller returns the
     * same response directly to the sender's current session.
     */
    @Transactional
    public ChatMessageResponse saveAndBroadcastMessage(
            SendChatMessageRequest request,
            Principal principal) {

        if (request == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A message request is required"
            );
        }

        if (request.conversationId() == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A conversation ID is required"
            );
        }

        User sender = getCurrentUser(principal);

        Conversation conversation =
            getConversation(request.conversationId());

        verifyMembership(conversation, sender);

        String body = request.body() == null
            ? ""
            : request.body().trim();

        if (body.isEmpty()) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Message cannot be empty"
            );
        }

        if (body.length() > MAX_MESSAGE_LENGTH) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Message cannot be longer than "
                    + MAX_MESSAGE_LENGTH
                    + " characters"
            );
        }

        ChatMessage message = ChatMessage.builder()
            .conversation(conversation)
            .sender(sender)
            .body(body)
            .build();

        ChatMessage savedMessage =
            chatMessageRepository.save(message);

        ChatMessageResponse response =
            ChatMessageResponse.fromMessage(savedMessage);

        User recipient = Objects.equals(
                sender.getId(),
                conversation.getManager().getId())
            ? conversation.getCleaner()
            : conversation.getManager();

        // Spring resolves this to the recipient's private subscription.
        messagingTemplate.convertAndSendToUser(
            recipient.getUsername(),
            "/queue/messages",
            response
        );

        return response;
    }

    /**
     * Loads a conversation or returns a 404 response.
     */
    private Conversation getConversation(Long conversationId) {
        if (conversationId == null) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "A conversation ID is required"
            );
        }

        return conversationRepository.findById(conversationId)
            .orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Conversation could not be found"
            ));
    }

    /**
     * Ensures that two users belong to the same organization.
     */
    private void validateSameOrganization(
            User firstUser,
            User secondUser) {

        if (firstUser.getOrganization() == null
                || secondUser.getOrganization() == null) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Both users must belong to an organization"
            );
        }

        Long firstOrganizationId =
            firstUser.getOrganization().getId();

        Long secondOrganizationId =
            secondUser.getOrganization().getId();

        if (!Objects.equals(
                firstOrganizationId,
                secondOrganizationId)) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You cannot chat with someone from another organization"
            );
        }
    }

    /**
     * Ensures that a user is one of the two conversation participants.
     */
    private void verifyMembership(
            Conversation conversation,
            User user) {

        boolean isManager = Objects.equals(
            conversation.getManager().getId(),
            user.getId()
        );

        boolean isCleaner = Objects.equals(
            conversation.getCleaner().getId(),
            user.getId()
        );

        if (!isManager && !isCleaner) {
            throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "You are not a member of this conversation"
            );
        }
    }
}
