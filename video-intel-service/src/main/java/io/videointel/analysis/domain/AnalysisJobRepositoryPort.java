package io.videointel.analysis.domain;

import java.util.Optional;
import java.util.UUID;

public interface AnalysisJobRepositoryPort {

    AnalysisJob save(AnalysisJob job);

    Optional<AnalysisJob> findById(UUID id);

    Optional<AnalysisJob> findByVideoId(UUID videoId);
}
