package io.videointel.analysis.api;

import io.videointel.analysis.application.AnalysisService;
import io.videointel.analysis.domain.AnalysisJob;
import io.videointel.analysis.domain.Transcript;
import io.videointel.shared.ownership.Owner;
import io.videointel.shared.ownership.OwnerType;
import io.videointel.shared.security.MissingOwnerException;
import io.videointel.shared.security.OwnerExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
class AnalysisController {

    private final AnalysisService analysisService;
    private final OwnerExtractor ownerExtractor;

    @PostMapping("/api/v1/videos/{videoId}/analysis")
    @ResponseStatus(HttpStatus.CREATED)
    AnalysisJobResponse requestAnalysis(@PathVariable UUID videoId) {
        return toResponse(analysisService.requestAnalysis(videoId));
    }

    @GetMapping("/api/v1/analysis-jobs/{id}")
    AnalysisJobResponse getJob(@PathVariable UUID id) {
        return toResponse(analysisService.getJob(id));
    }

    @GetMapping({"/api/v1/videos/{videoId}/transcript", "/api/videos/{videoId}/transcript"})
    TranscriptResponse getTranscript(
            @PathVariable UUID videoId,
            @RequestHeader(value = "X-Owner-Id", required = false) UUID ownerIdHeader,
            @RequestHeader(value = "X-Owner-Type", required = false) OwnerType ownerTypeHeader,
            @RequestParam(value = "ownerId", required = false) UUID ownerIdParam,
            @RequestParam(value = "ownerType", required = false) OwnerType ownerTypeParam
    ) {
        UUID effectiveOwnerId = ownerIdHeader != null ? ownerIdHeader : ownerIdParam;
        OwnerType effectiveOwnerType = ownerTypeHeader != null ? ownerTypeHeader : ownerTypeParam;

        Owner requester = ownerExtractor.extract(effectiveOwnerId, effectiveOwnerType).orElse(null);

        Transcript transcript = analysisService.getTranscript(videoId, requester);
        return toTranscriptResponse(transcript);
    }

    private AnalysisJobResponse toResponse(AnalysisJob job) {
        return new AnalysisJobResponse(
                job.id(), job.videoId(), job.status().name(), job.failureReason(),
                job.createdAt(), job.updatedAt()
        );
    }

    private TranscriptResponse toTranscriptResponse(Transcript transcript) {
        List<TranscriptResponse.SegmentResponse> segments = transcript.segments() == null
                ? List.of()
                : transcript.segments().stream()
                        .map(s -> new TranscriptResponse.SegmentResponse(s.start(), s.end(), s.text()))
                        .toList();

        return new TranscriptResponse(transcript.videoId(), transcript.text(), segments);
    }
}
