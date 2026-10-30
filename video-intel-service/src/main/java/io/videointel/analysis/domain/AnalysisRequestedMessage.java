package io.videointel.analysis.domain;

import java.util.UUID;

public record AnalysisRequestedMessage(UUID jobId, UUID videoId, String storageRef) {

    public static final String TYPE = "analysis-requested";
}
