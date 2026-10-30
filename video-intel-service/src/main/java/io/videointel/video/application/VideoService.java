package io.videointel.video.application;

import io.videointel.shared.ownership.Owner;
import io.videointel.user.application.OwnerLookupPort;
import io.videointel.video.domain.IllegalVideoStateTransitionException;
import io.videointel.video.domain.UnknownOwnerException;
import io.videointel.video.domain.Video;
import io.videointel.video.domain.VideoNotFoundException;
import io.videointel.video.domain.VideoRepositoryPort;
import io.videointel.video.domain.VideoStatus;
import io.videointel.video.domain.VideoStoragePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VideoService {

    private final VideoRepositoryPort videoRepository;
    private final OwnerLookupPort ownerLookupPort;
    private final VideoStoragePort videoStoragePort;

    @Transactional
    public Video register(Owner owner, String title) {
        if (!ownerLookupPort.exists(owner)) {
            throw new UnknownOwnerException(owner.id());
        }
        Video video = Video.register(owner, title);
        return videoRepository.save(video);
    }

    @Transactional(readOnly = true)
    public Video get(UUID id) {
        return videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException(id));
    }

    @Transactional
    public void delete(UUID id) {
        if (!videoRepository.existsById(id)) {
            throw new VideoNotFoundException(id);
        }
        videoRepository.deleteById(id);
    }

    @Transactional
    public void markUploading(UUID id) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException(id));
        video.markUploading();
        videoRepository.save(video);
    }

    @Transactional
    public void markUploaded(UUID id, String storageRef) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException(id));
        video.markUploaded(storageRef);
        videoRepository.save(video);
    }

    @Transactional
    public void markFailed(UUID id, String reason) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException(id));
        video.markFailed(reason);
        videoRepository.save(video);
    }

    @Transactional
    public String getUploadUrl(UUID id) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException(id));

        if (video.status() == VideoStatus.AWAITING_UPLOAD) {
            video.markUploading();
            videoRepository.save(video);
        } else if (video.status() != VideoStatus.UPLOADING) {
            throw new IllegalVideoStateTransitionException(
                    "Cannot request an upload URL for video in status " + video.status()
            );
        }

        return videoStoragePort.presignedUploadUrl(id);
    }

    @Transactional(readOnly = true)
    public String getPlaybackUrl(UUID id) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException(id));

        if (video.status() != VideoStatus.UPLOADED || video.storageRef() == null) {
            throw new IllegalVideoStateTransitionException(
                    "Cannot get a playback URL for video in status " + video.status()
            );
        }

        return videoStoragePort.presignedPlaybackUrl(video.storageRef());
    }

    @Transactional
    public Video completeUpload(UUID id) {
        Video video = videoRepository.findById(id).orElseThrow(() -> new VideoNotFoundException(id));

        String storageRef = id.toString();
        if (videoStoragePort.exists(storageRef)) {
            video.markUploaded(storageRef);
        } else {
            video.markFailed("Upload not found in storage");
        }

        return videoRepository.save(video);
    }
}
