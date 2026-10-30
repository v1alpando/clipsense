package io.videointel.analysis.domain;

import java.util.Optional;
import java.util.UUID;

public interface TranscriptRepositoryPort {

    Transcript save(Transcript transcript);

    Optional<Transcript> findByVideoId(UUID videoId);
}
