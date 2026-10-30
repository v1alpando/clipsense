package io.videointel.video.domain;

import io.videointel.shared.ownership.Owner;
import io.videointel.shared.ownership.OwnerType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VideoTest {

    @Test
    void registerCreatesVideoAwaitingUpload() {
        Owner owner = new Owner.RegisteredUser(UUID.randomUUID());

        Video video = Video.register(owner, "My video");

        assertThat(video.id()).isNotNull();
        assertThat(video.owner()).isEqualTo(owner);
        assertThat(video.title()).isEqualTo("My video");
        assertThat(video.status()).isEqualTo(VideoStatus.AWAITING_UPLOAD);
        assertThat(video.storageRef()).isNull();
        assertThat(video.createdAt()).isEqualTo(video.updatedAt());
    }

    @Test
    void registerSupportsAnonymousSessionOwner() {
        Owner owner = new Owner.AnonymousSession(UUID.randomUUID());

        Video video = Video.register(owner, "Anonymous video");

        assertThat(video.owner()).isEqualTo(owner);
        assertThat(video.owner().type()).isEqualTo(OwnerType.ANONYMOUS_SESSION);
    }

    @Test
    void registerRejectsNullOwner() {
        assertThatThrownBy(() -> Video.register(null, "My video"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerRejectsBlankTitle() {
        Owner owner = new Owner.RegisteredUser(UUID.randomUUID());

        assertThatThrownBy(() -> Video.register(owner, " "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void fullLifecycleToUploadedUpdatesStatusStorageRefAndTimestamp() {
        Video video = Video.register(new Owner.RegisteredUser(UUID.randomUUID()), "My video");
        var registeredAt = video.updatedAt();

        video.markUploading();
        assertThat(video.status()).isEqualTo(VideoStatus.UPLOADING);
        assertThat(video.updatedAt()).isAfterOrEqualTo(registeredAt);

        video.markUploaded("s3://bucket/video.mp4");
        assertThat(video.status()).isEqualTo(VideoStatus.UPLOADED);
        assertThat(video.storageRef()).isEqualTo("s3://bucket/video.mp4");
        assertThat(video.failedReason()).isNull();
    }

    @Test
    void lifecycleToFailedRecordsReason() {
        Video video = Video.register(new Owner.RegisteredUser(UUID.randomUUID()), "My video");
        video.markUploading();

        video.markFailed("upload never landed in storage");

        assertThat(video.status()).isEqualTo(VideoStatus.FAILED);
        assertThat(video.failedReason()).isEqualTo("upload never landed in storage");
        assertThat(video.storageRef()).isNull();
    }

    @Test
    void markUploadingRejectsNonAwaitingUploadStatus() {
        Video video = Video.register(new Owner.RegisteredUser(UUID.randomUUID()), "My video");
        video.markUploading();

        assertThatThrownBy(video::markUploading)
                .isInstanceOf(IllegalVideoStateTransitionException.class);
    }

    @Test
    void markUploadedRejectsNonUploadingStatus() {
        Video video = Video.register(new Owner.RegisteredUser(UUID.randomUUID()), "My video");

        assertThatThrownBy(() -> video.markUploaded("s3://bucket/video.mp4"))
                .isInstanceOf(IllegalVideoStateTransitionException.class);
    }

    @Test
    void markFailedRejectsNonUploadingStatus() {
        Video video = Video.register(new Owner.RegisteredUser(UUID.randomUUID()), "My video");

        assertThatThrownBy(() -> video.markFailed("some reason"))
                .isInstanceOf(IllegalVideoStateTransitionException.class);
    }

    @Test
    void terminalStatusesRejectFurtherTransitions() {
        Video uploaded = Video.register(new Owner.RegisteredUser(UUID.randomUUID()), "My video");
        uploaded.markUploading();
        uploaded.markUploaded("s3://bucket/video.mp4");

        assertThatThrownBy(() -> uploaded.markFailed("too late"))
                .isInstanceOf(IllegalVideoStateTransitionException.class);

        Video failed = Video.register(new Owner.RegisteredUser(UUID.randomUUID()), "My video");
        failed.markUploading();
        failed.markFailed("boom");

        assertThatThrownBy(() -> failed.markUploaded("s3://bucket/video.mp4"))
                .isInstanceOf(IllegalVideoStateTransitionException.class);
    }
}
