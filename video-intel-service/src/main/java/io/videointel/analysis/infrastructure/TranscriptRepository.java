package io.videointel.analysis.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface TranscriptRepository extends JpaRepository<TranscriptEntity, UUID> {

    Optional<TranscriptEntity> findByVideoId(UUID videoId);
}
