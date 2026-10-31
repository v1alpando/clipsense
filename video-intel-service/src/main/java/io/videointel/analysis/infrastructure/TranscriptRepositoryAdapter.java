package io.videointel.analysis.infrastructure;

import io.videointel.analysis.domain.Transcript;
import io.videointel.analysis.domain.TranscriptRepositoryPort;
import io.videointel.analysis.domain.TranscriptSegment;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class TranscriptRepositoryAdapter implements TranscriptRepositoryPort {

    private final TranscriptRepository jpaRepository;

    @Override
    public Transcript save(Transcript transcript) {
        List<TranscriptEntity.SegmentRecord> entitySegments = transcript.segments() == null
                ? List.of()
                : transcript.segments().stream()
                        .map(s -> new TranscriptEntity.SegmentRecord(s.start(), s.end(), s.text()))
                        .toList();

        TranscriptEntity entity = new TranscriptEntity(
                transcript.id(), transcript.videoId(), transcript.text(), entitySegments, transcript.createdAt()
        );
        jpaRepository.save(entity);
        return transcript;
    }

    @Override
    public Optional<Transcript> findByVideoId(UUID videoId) {
        return jpaRepository.findByVideoId(videoId).map(this::toDomain);
    }

    private Transcript toDomain(TranscriptEntity entity) {
        List<TranscriptSegment> segments = entity.getSegments() == null
                ? List.of()
                : entity.getSegments().stream()
                        .map(s -> new TranscriptSegment(s.start(), s.end(), s.text()))
                        .toList();

        return Transcript.reconstruct(
                entity.getId(), entity.getVideoId(), entity.getText(), segments, entity.getCreatedAt()
        );
    }
}
