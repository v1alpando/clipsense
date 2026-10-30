package io.videointel.analysis.infrastructure;

import io.videointel.analysis.domain.AnalysisJob;
import io.videointel.analysis.domain.AnalysisJobRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class AnalysisJobRepositoryAdapter implements AnalysisJobRepositoryPort {

    private final AnalysisJobRepository jpaRepository;

    @Override
    public AnalysisJob save(AnalysisJob job) {
        AnalysisJobEntity entity = jpaRepository.findById(job.id()).orElseGet(AnalysisJobEntity::new);
        entity.setId(job.id());
        entity.setVideoId(job.videoId());
        entity.setStatus(job.status());
        entity.setCreatedAt(job.createdAt());
        entity.setUpdatedAt(job.updatedAt());
        entity.setFailureReason(job.failureReason());
        jpaRepository.save(entity);
        return job;
    }

    @Override
    public Optional<AnalysisJob> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<AnalysisJob> findByVideoId(UUID videoId) {
        return jpaRepository.findByVideoId(videoId).map(this::toDomain);
    }

    private AnalysisJob toDomain(AnalysisJobEntity entity) {
        return AnalysisJob.reconstruct(
                entity.getId(), entity.getVideoId(), entity.getStatus(),
                entity.getCreatedAt(), entity.getUpdatedAt(), entity.getFailureReason()
        );
    }
}
