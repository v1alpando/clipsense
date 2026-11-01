package io.videointel.analysis.infrastructure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
record AnalysisResultMessage(
        UUID jobId,
        boolean success,
        String transcriptText,
        String failureReason,
        List<TranscriptSegmentDto> segments,
        @JsonProperty("analysis") @JsonAlias("analysisData") AnalysisPayloadDto analysis
) {

    AnalysisResultMessage(UUID jobId, boolean success, String transcriptText, String failureReason) {
        this(jobId, success, transcriptText, failureReason, List.of(), null);
    }

    AnalysisResultMessage(UUID jobId, boolean success, String transcriptText, String failureReason, List<TranscriptSegmentDto> segments) {
        this(jobId, success, transcriptText, failureReason, segments, null);
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record TranscriptSegmentDto(double start, double end, String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record AnalysisPayloadDto(
            String summary,
            HookDto hook,
            @JsonProperty("key_moments") @JsonAlias("keyMoments") List<KeyMomentDto> keyMoments,
            List<String> takeaways
    ) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        record HookDto(
                int score,
                String type,
                String critique,
                String suggestion
        ) {
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        record KeyMomentDto(
                double timestamp,
                String title,
                String description
        ) {
        }
    }
}
