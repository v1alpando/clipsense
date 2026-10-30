package io.videointel.user.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class AnonymousSession {

    private static final Duration DEFAULT_TTL = Duration.ofDays(7);

    private final UUID id;
    private final Instant createdAt;
    private final Instant expiresAt;

    public static AnonymousSession create() {
        Instant now = Instant.now();
        return new AnonymousSession(UUID.randomUUID(), now, now.plus(DEFAULT_TTL));
    }

    public static AnonymousSession reconstruct(UUID id, Instant createdAt, Instant expiresAt) {
        return new AnonymousSession(id, createdAt, expiresAt);
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt);
    }
}
