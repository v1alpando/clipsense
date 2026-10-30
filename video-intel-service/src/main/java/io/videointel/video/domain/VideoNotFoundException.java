package io.videointel.video.domain;

import io.videointel.shared.exception.NotFoundException;

import java.util.UUID;

public class VideoNotFoundException extends NotFoundException {

    public VideoNotFoundException(UUID id) {
        super("Video not found: " + id);
    }
}
