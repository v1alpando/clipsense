package io.videointel.analysis.infrastructure;

import io.videointel.analysis.domain.Transcript;
import io.videointel.analysis.domain.TranscriptRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class TranscriptRepositoryAdapter implements TranscriptRepositoryPort {

    private final TranscriptRepository jpaRepository;

    @Override
    public Transcript save(Transcript transcript) {
        TranscriptEntity entity = new TranscriptEntity(
                transcript.id(), transcript.videoId(), transcript.text(), transcript.createdAt()
        );
        jpaRepository.save(entity);
        return transcript;
    }

    @Override
    public Optional<Transcript> findByVideoId(UUID videoId) {
        return jpaRepository.findByVideoId(videoId).map(this::toDomain);
    }

    private Transcript toDomain(TranscriptEntity entity) {
        return Transcript.reconstruct(entity.getId(), entity.getVideoId(), entity.getText(), entity.getCreatedAt());
    }
}
