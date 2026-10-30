package io.videointel.user.api;

import java.time.Instant;
import java.util.UUID;

record AnonymousSessionResponse(
        UUID id,
        Instant createdAt,
        Instant expiresAt
) {
}
