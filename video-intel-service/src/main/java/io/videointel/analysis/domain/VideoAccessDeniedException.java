package io.videointel.analysis.domain;

import io.videointel.shared.exception.ForbiddenException;

import java.util.UUID;

public class VideoAccessDeniedException extends ForbiddenException {

    public VideoAccessDeniedException(UUID videoId) {
        super("Access denied to video: " + videoId);
    }
}
