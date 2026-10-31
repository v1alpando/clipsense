package io.videointel.analysis.infrastructure;

import java.util.List;
import java.util.UUID;

record AnalysisResultMessage(
        UUID jobId,
        boolean success,
        String transcriptText,
        String failureReason,
        List<TranscriptSegmentDto> segments
) {

    AnalysisResultMessage(UUID jobId, boolean success, String transcriptText, String failureReason) {
        this(jobId, success, transcriptText, failureReason, List.of());
    }

    record TranscriptSegmentDto(double start, double end, String text) {
    }
}
