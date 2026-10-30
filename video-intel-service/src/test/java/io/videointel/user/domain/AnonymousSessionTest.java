package io.videointel.user.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;

class AnonymousSessionTest {

    @Test
    void createSetsExpiryInTheFuture() {
        AnonymousSession session = AnonymousSession.create();

        assertThat(session.id()).isNotNull();
        assertThat(session.expiresAt()).isAfter(session.createdAt());
    }

    @Test
    void isExpiredReflectsExpiresAt() {
        AnonymousSession session = AnonymousSession.create();

        assertThat(session.isExpired(session.createdAt())).isFalse();
        assertThat(session.isExpired(session.expiresAt().plus(1, ChronoUnit.SECONDS))).isTrue();
    }

    @Test
    void reconstructRebuildsSessionAsIs() {
        Instant createdAt = Instant.parse("2026-01-01T00:00:00Z");
        Instant expiresAt = Instant.parse("2026-01-08T00:00:00Z");
        var id = java.util.UUID.randomUUID();

        AnonymousSession session = AnonymousSession.reconstruct(id, createdAt, expiresAt);

        assertThat(session.id()).isEqualTo(id);
        assertThat(session.createdAt()).isEqualTo(createdAt);
        assertThat(session.expiresAt()).isEqualTo(expiresAt);
    }
}
