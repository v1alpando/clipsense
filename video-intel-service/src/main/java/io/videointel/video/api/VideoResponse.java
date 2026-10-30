package io.videointel.video.api;

import java.time.Instant;
import java.util.UUID;

record VideoResponse(
        UUID id,
        String ownerType,
        UUID ownerId,
        String title,
        String status,
        String storageRef,
        Instant createdAt,
        Instant updatedAt,
        String failedReason
) {
}
