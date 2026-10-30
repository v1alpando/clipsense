package io.videointel.analysis.domain;

import io.videointel.shared.exception.NotFoundException;

import java.util.UUID;

public class AnalysisJobNotFoundException extends NotFoundException {
    public AnalysisJobNotFoundException(UUID id) {
        super("AnalysisJob not found: " + id);
    }
}
