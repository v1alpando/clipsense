package io.videointel.chat.api;

import io.videointel.chat.application.ChatService;
import io.videointel.chat.domain.ChatMessage;
import io.videointel.chat.domain.ChatSession;
import io.videointel.shared.ownership.Owner;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class ChatController {

    private final ChatService chatService;

    @PostMapping("/api/v1/videos/{videoId}/chat-sessions")
    @ResponseStatus(HttpStatus.CREATED)
    ChatSessionResponse createSession(@PathVariable UUID videoId,
                                       @Valid @RequestBody CreateChatSessionRequest request) {
        Owner owner = Owner.of(request.ownerType(), request.ownerId());
        ChatSession session = chatService.createSession(videoId, owner);
        return toResponse(session);
    }

    @PostMapping("/api/v1/chat-sessions/{id}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    ChatMessageResponse sendMessage(@PathVariable UUID id, @Valid @RequestBody SendMessageRequest request) {
        ChatMessage reply = chatService.sendMessage(id, request.content());
        return toResponse(reply);
    }

    @GetMapping("/api/v1/chat-sessions/{id}/messages")
    List<ChatMessageResponse> getMessages(@PathVariable UUID id) {
        return chatService.getMessages(id).stream().map(this::toResponse).toList();
    }

    private ChatSessionResponse toResponse(ChatSession session) {
        return new ChatSessionResponse(
                session.id(), session.videoId(), session.owner().type().name(),
                session.owner().id(), session.createdAt()
        );
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return new ChatMessageResponse(
                message.id(), message.sessionId(), message.role().name(),
                message.content(), message.createdAt()
        );
    }
}
