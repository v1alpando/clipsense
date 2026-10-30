package io.videointel.video.infrastructure;

import io.videointel.video.domain.VideoStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

interface VideoRepository extends JpaRepository<VideoEntity, UUID> {

    List<VideoEntity> findByStatusAndUpdatedAtBefore(VideoStatus status, Instant threshold);
}
