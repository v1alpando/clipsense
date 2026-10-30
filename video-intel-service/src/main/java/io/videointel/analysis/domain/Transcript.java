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
public final class Transcript {

    private final UUID id;
    private final UUID videoId;
    private final String text;
    private final Instant createdAt;

    public static Transcript create(UUID videoId, String text) {
        if (videoId == null) {
            throw new IllegalArgumentException("videoId must not be null");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        return new Transcript(UUID.randomUUID(), videoId, text, Instant.now());
    }

    public static Transcript reconstruct(UUID id, UUID videoId, String text, Instant createdAt) {
        return new Transcript(id, videoId, text, createdAt);
    }
}
