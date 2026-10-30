package io.videointel.chat.api;

import java.time.Instant;
import java.util.UUID;

record ChatSessionResponse(
        UUID id,
        UUID videoId,
        String ownerType,
        UUID ownerId,
        Instant createdAt
) {
}
