package io.videointel.analysis.domain;

import io.videointel.shared.exception.NotFoundException;

import java.util.UUID;

public class AnalysisDataNotFoundException extends NotFoundException {

    public AnalysisDataNotFoundException(UUID videoId) {
        super("Analysis data not found for video: " + videoId);
    }
}
