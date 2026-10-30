package io.videointel.analysis.infrastructure;

import java.util.UUID;

record AnalysisResultMessage(UUID jobId, boolean success, String transcriptText, String failureReason) {
}
