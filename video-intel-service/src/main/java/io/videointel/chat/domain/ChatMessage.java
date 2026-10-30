package io.videointel.chat.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatMessage {

    private final UUID id;
    private final UUID sessionId;
    private final MessageRole role;
    private final String content;
    private final Instant createdAt;

    public static ChatMessage create(UUID sessionId, MessageRole role, String content) {
        if (sessionId == null) {
            throw new IllegalArgumentException("sessionId must not be null");
        }
        if (role == null) {
            throw new IllegalArgumentException("role must not be null");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content must not be blank");
        }
        return new ChatMessage(UUID.randomUUID(), sessionId, role, content, Instant.now());
    }

    public static ChatMessage reconstruct(UUID id, UUID sessionId, MessageRole role, String content, Instant createdAt) {
        return new ChatMessage(id, sessionId, role, content, createdAt);
    }
}
