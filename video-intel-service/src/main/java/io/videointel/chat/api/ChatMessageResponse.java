package io.videointel.chat.api;

import java.time.Instant;
import java.util.UUID;

record ChatMessageResponse(
        UUID id,
        UUID sessionId,
        String role,
        String content,
        Instant createdAt
) {
}
