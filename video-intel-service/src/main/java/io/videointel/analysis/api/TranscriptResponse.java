package io.videointel.analysis.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record TranscriptResponse(
        UUID videoId,
        String text,
        List<SegmentResponse> segments
) {

    @JsonProperty("fullText")
    public String fullText() {
        return text;
    }

    public record SegmentResponse(double start, double end, String text) {
    }
}
