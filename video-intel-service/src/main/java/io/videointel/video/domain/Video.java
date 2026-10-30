package io.videointel.video.domain;

import io.videointel.shared.ownership.Owner;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Video {

    private final UUID id;
    private final Owner owner;
    private final String title;
    private VideoStatus status;
    private String storageRef;
    private final Instant createdAt;
    private Instant updatedAt;
    private String failedReason;

    public static Video register(Owner owner, String title) {
        if (owner == null) {
            throw new IllegalArgumentException("owner must not be null");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("title must not be blank");
        }
        Instant now = Instant.now();
        return new Video(UUID.randomUUID(), owner, title, VideoStatus.AWAITING_UPLOAD, null, now, now, null);
    }

    public static Video reconstruct(UUID id, Owner owner, String title, VideoStatus status,
                                     String storageRef, Instant createdAt, Instant updatedAt,
                                     String failedReason) {
        return new Video(id, owner, title, status, storageRef, createdAt, updatedAt, failedReason);
    }

    public void markUploading() {
        requireStatus(VideoStatus.AWAITING_UPLOAD);
        this.updatedAt = Instant.now();
        this.status = VideoStatus.UPLOADING;
    }

    public void markUploaded(String storageRef) {
        requireStatus(VideoStatus.UPLOADING);
        this.updatedAt = Instant.now();
        this.status = VideoStatus.UPLOADED;
        this.storageRef = storageRef;
    }

    public void markFailed(String reason) {
        requireStatus(VideoStatus.UPLOADING);
        this.updatedAt = Instant.now();
        this.status = VideoStatus.FAILED;
        this.failedReason = reason;
    }

    private void requireStatus(VideoStatus expected) {
        if (status != expected) {
            throw new IllegalVideoStateTransitionException(
                    "Cannot transition video from " + status + ": expected status " + expected
            );
        }
    }
}
