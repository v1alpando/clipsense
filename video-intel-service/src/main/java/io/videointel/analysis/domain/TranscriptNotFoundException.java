package io.videointel.analysis.domain;

import io.videointel.shared.exception.NotFoundException;

import java.util.UUID;

public class TranscriptNotFoundException extends NotFoundException {

    public TranscriptNotFoundException(UUID videoId) {
        super("Transcript not found for video: " + videoId);
    }
}
