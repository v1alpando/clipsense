package io.videointel.analysis.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record VideoInsightsResponse(
        UUID videoId,
        ExecutiveSummary summary,
        TranscriptResponse.HookResponse hook,
        List<TranscriptResponse.KeyMomentResponse> keyMoments,
        List<String> takeaways,
        @JsonProperty("rawSummary") String rawSummary
) {

    public record ExecutiveSummary(
            String tldr,
            List<String> takeaways
    ) {
    }
}
