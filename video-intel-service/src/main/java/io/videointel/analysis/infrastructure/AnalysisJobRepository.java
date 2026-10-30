package io.videointel.analysis.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface AnalysisJobRepository extends JpaRepository<AnalysisJobEntity, UUID> {

    Optional<AnalysisJobEntity> findByVideoId(UUID videoId);
}
