package io.videointel.video.application;

import io.videointel.video.domain.VideoRepositoryPort;
import io.videointel.video.domain.VideoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class VideoLookupService implements VideoLookupPort {
    private final VideoRepositoryPort videoRepositoryPort;

    @Override
    public Optional<VideoView> findById(UUID id) {
        return videoRepositoryPort.findById(id)
                .map(video -> new VideoView(video.id(), video.status() == VideoStatus.UPLOADED, video.storageRef()));
    }
}
