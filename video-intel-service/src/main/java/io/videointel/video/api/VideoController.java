package io.videointel.video.api;

import io.videointel.shared.ownership.Owner;
import io.videointel.video.application.VideoService;
import io.videointel.video.domain.Video;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/videos")
@RequiredArgsConstructor
class VideoController {

    private final VideoService videoService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    VideoResponse create(@Valid @RequestBody CreateVideoRequest request) {
        Owner owner = Owner.of(request.ownerType(), request.ownerId());
        Video video = videoService.register(owner, request.title());
        return toResponse(video);
    }

    @GetMapping("/{id}")
    VideoResponse get(@PathVariable UUID id) {
        return toResponse(videoService.get(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(@PathVariable UUID id) {
        videoService.delete(id);
    }

    @PostMapping("/{id}/upload-url")
    UploadUrlResponse getUploadUrl(@PathVariable UUID id) {
        return new UploadUrlResponse(videoService.getUploadUrl(id));
    }

    @PostMapping("/{id}/complete")
    VideoResponse completeUpload(@PathVariable UUID id) {
        return toResponse(videoService.completeUpload(id));
    }

    @GetMapping("/{id}/playback-url")
    PlaybackUrlResponse getPlaybackUrl(@PathVariable UUID id) {
        return new PlaybackUrlResponse(videoService.getPlaybackUrl(id));
    }

    private VideoResponse toResponse(Video video) {
        return new VideoResponse(
                video.id(), video.owner().type().name(), video.owner().id(), video.title(),
                video.status().name(), video.storageRef(), video.createdAt(), video.updatedAt(), video.failedReason()
        );
    }
}
