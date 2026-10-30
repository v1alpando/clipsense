package io.videointel.video.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VideoRepositoryPort {

    Video save(Video video);

    Optional<Video> findById(UUID id);

    boolean existsById(UUID id);

    void deleteById(UUID id);

    List<Video> findStaleUploading(Instant olderThan);
}
