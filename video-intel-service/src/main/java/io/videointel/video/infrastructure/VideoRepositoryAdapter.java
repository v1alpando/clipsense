package io.videointel.video.infrastructure;

import io.videointel.shared.ownership.Owner;
import io.videointel.video.domain.Video;
import io.videointel.video.domain.VideoRepositoryPort;
import io.videointel.video.domain.VideoStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class VideoRepositoryAdapter implements VideoRepositoryPort {

    private final VideoRepository jpaRepository;

    @Override
    public Video save(Video video) {
        VideoEntity entity = jpaRepository.findById(video.id()).orElseGet(VideoEntity::new);
        entity.setId(video.id());
        entity.setOwnerType(video.owner().type());
        entity.setOwnerId(video.owner().id());
        entity.setTitle(video.title());
        entity.setStatus(video.status());
        entity.setStorageRef(video.storageRef());
        entity.setCreatedAt(video.createdAt());
        entity.setUpdatedAt(video.updatedAt());
        entity.setFailedReason(video.failedReason());
        jpaRepository.save(entity);
        return video;
    }

    @Override
    public Optional<Video> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public List<Video> findStaleUploading(Instant olderThan) {
        return jpaRepository.findByStatusAndUpdatedAtBefore(VideoStatus.UPLOADING, olderThan)
                .stream()
                .map(this::toDomain)
                .toList();
    }

    private Video toDomain(VideoEntity entity) {
        Owner owner = Owner.of(entity.getOwnerType(), entity.getOwnerId());
        return Video.reconstruct(
                entity.getId(), owner, entity.getTitle(), entity.getStatus(),
                entity.getStorageRef(), entity.getCreatedAt(), entity.getUpdatedAt(),
                entity.getFailedReason()
        );
    }
}
