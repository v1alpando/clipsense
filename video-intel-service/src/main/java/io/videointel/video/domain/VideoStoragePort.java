package io.videointel.video.domain;

import java.util.UUID;

public interface VideoStoragePort {

    String presignedUploadUrl(UUID videoId);

    String presignedPlaybackUrl(String storageRef);

    boolean exists(String storageRef);
}
