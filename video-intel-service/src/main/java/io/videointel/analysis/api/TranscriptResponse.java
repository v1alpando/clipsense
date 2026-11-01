package io.videointel.analysis.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

public record TranscriptResponse(
        UUID videoId,
        String fullText,
        List<SegmentResponse> segments,
        AnalysisResponse analysis
) {

    public TranscriptResponse(UUID videoId, String fullText, List<SegmentResponse> segments) {
        this(videoId, fullText, segments, null);
    }

    @JsonProperty("text")
    public String text() {
        return fullText;
    }

    public record SegmentResponse(double start, double end, String text) {
    }

    public record AnalysisResponse(
            String summary,
            HookResponse hook,
            List<KeyMomentResponse> keyMoments,
            List<String> takeaways
    ) {
    }

    public record HookResponse(
            int score,
            String type,
            String critique,
            String suggestion
    ) {
        @JsonProperty("hookType")
        public String hookType() {
            return type;
        }
    }

    public record KeyMomentResponse(
            double timestamp,
            String title,
            String description
    ) {
        @JsonProperty("id")
        public String id() {
            return "moment-" + (long) (timestamp * 1000);
        }
    }
}
