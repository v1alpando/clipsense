package io.videointel.analysis.api;

import io.videointel.analysis.application.AnalysisService;
import io.videointel.analysis.domain.AnalysisJob;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
class AnalysisController {

    private final AnalysisService analysisService;

    @PostMapping("/api/v1/videos/{videoId}/analysis")
    @ResponseStatus(HttpStatus.CREATED)
    AnalysisJobResponse requestAnalysis(@PathVariable UUID videoId) {
        return toResponse(analysisService.requestAnalysis(videoId));
    }

    @GetMapping("/api/v1/analysis-jobs/{id}")
    AnalysisJobResponse getJob(@PathVariable UUID id) {
        return toResponse(analysisService.getJob(id));
    }

    private AnalysisJobResponse toResponse(AnalysisJob job) {
        return new AnalysisJobResponse(
                job.id(), job.videoId(), job.status().name(), job.failureReason(),
                job.createdAt(), job.updatedAt()
        );
    }
}
