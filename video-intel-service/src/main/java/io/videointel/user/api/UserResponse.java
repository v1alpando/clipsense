package io.videointel.user.api;

import java.time.Instant;
import java.util.UUID;

record UserResponse(
        UUID id,
        String email,
        Instant createdAt
) {
}
