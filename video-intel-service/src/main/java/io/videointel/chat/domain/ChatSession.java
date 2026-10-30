package io.videointel.chat.domain;

import io.videointel.shared.ownership.Owner;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class ChatSession {

    private final UUID id;
    private final UUID videoId;
    private final Owner owner;
    private final Instant createdAt;

    public static ChatSession start(UUID videoId, Owner owner) {
        if (videoId == null) {
            throw new IllegalArgumentException("videoId must not be null");
        }
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        return new ChatSession(UUID.randomUUID(), videoId, owner, Instant.now());
    }

    public static ChatSession reconstruct(UUID id, UUID videoId, Owner owner, Instant createdAt) {
        return new ChatSession(id, videoId, owner, createdAt);
    }
}
