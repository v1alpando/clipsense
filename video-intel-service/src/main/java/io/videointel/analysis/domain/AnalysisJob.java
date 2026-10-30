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
public final class AnalysisJob {

    private final UUID id;
    private final UUID videoId;
    private AnalysisStatus status;
    private final Instant createdAt;
    private Instant updatedAt;
    private String failureReason;

    public static AnalysisJob request(UUID videoId) {
        if (videoId == null) {
            throw new IllegalArgumentException("videoId must not be null");
        }
        Instant now = Instant.now();
        return new AnalysisJob(UUID.randomUUID(), videoId, AnalysisStatus.QUEUED, now, now, null);
    }

    public static AnalysisJob reconstruct(UUID id, UUID videoId, AnalysisStatus status,
                                           Instant createdAt, Instant updatedAt, String failureReason) {
        return new AnalysisJob(id, videoId, status, createdAt, updatedAt, failureReason);
    }

    public void markRunning() {
        requireStatus(AnalysisStatus.QUEUED);
        this.updatedAt = Instant.now();
        this.status = AnalysisStatus.RUNNING;
    }

    public void markCompleted() {
        requireStatus(AnalysisStatus.RUNNING);
        this.updatedAt = Instant.now();
        this.status = AnalysisStatus.COMPLETED;
    }

    public void markFailed(String reason) {
        requireStatus(AnalysisStatus.RUNNING);
        this.updatedAt = Instant.now();
        this.status = AnalysisStatus.FAILED;
        this.failureReason = reason;
    }

    private void requireStatus(AnalysisStatus expected) {
        if (status != expected) {
            throw new IllegalAnalysisStateTransitionException(
                    "Cannot transition analysis job from " + status + ": expected status " + expected
            );
        }
    }
}
