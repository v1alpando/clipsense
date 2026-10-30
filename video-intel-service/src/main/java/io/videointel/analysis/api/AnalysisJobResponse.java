package io.videointel.analysis.api;

import java.time.Instant;
import java.util.UUID;

record AnalysisJobResponse(
        UUID id,
        UUID videoId,
        String status,
        String failureReason,
        Instant createdAt,
        Instant updatedAt
) {
}
