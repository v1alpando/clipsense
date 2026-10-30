package io.videointel.analysis.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class OutboxMessage {

    private final UUID id;
    private final String messageType;
    private final String payload;
    private final Instant createdAt;
    private Instant processedAt;

    public static OutboxMessage create(String messageType, String payload) {
        if (messageType == null || messageType.isBlank()) {
            throw new IllegalArgumentException("messageType must not be blank");
        }
        if (payload == null) {
            throw new IllegalArgumentException("payload must not be null");
        }
        return new OutboxMessage(UUID.randomUUID(), messageType, payload, Instant.now(), null);
    }

    public static OutboxMessage reconstruct(UUID id, String messageType, String payload,
                                             Instant createdAt, Instant processedAt) {
        return new OutboxMessage(id, messageType, payload, createdAt, processedAt);
    }

    public void markProcessed() {
        this.processedAt = Instant.now();
    }
}
