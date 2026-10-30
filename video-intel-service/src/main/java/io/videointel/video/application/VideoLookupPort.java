package io.videointel.video.application;

import java.util.Optional;
import java.util.UUID;

public interface VideoLookupPort {
    Optional<VideoView> findById(UUID id);
}
