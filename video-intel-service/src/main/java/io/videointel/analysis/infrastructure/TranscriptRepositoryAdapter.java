package io.videointel.analysis.infrastructure;

import io.videointel.analysis.domain.Hook;
import io.videointel.analysis.domain.KeyMoment;
import io.videointel.analysis.domain.Transcript;
import io.videointel.analysis.domain.TranscriptRepositoryPort;
import io.videointel.analysis.domain.TranscriptSegment;
import io.videointel.analysis.domain.VideoAnalysisData;
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

        TranscriptEntity.AnalysisDataRecord entityAnalysis = toEntityAnalysis(transcript.analysisData());

        TranscriptEntity entity = new TranscriptEntity(
                transcript.id(), transcript.videoId(), transcript.text(),
                entitySegments, entityAnalysis, transcript.createdAt()
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

        VideoAnalysisData analysisData = toDomainAnalysis(entity.getAnalysisData());

        return Transcript.reconstruct(
                entity.getId(), entity.getVideoId(), entity.getText(),
                segments, analysisData, entity.getCreatedAt()
        );
    }

    private TranscriptEntity.AnalysisDataRecord toEntityAnalysis(VideoAnalysisData domain) {
        if (domain == null) {
            return null;
        }

        TranscriptEntity.AnalysisDataRecord.HookRecord hookRecord = null;
        if (domain.hook() != null) {
            hookRecord = new TranscriptEntity.AnalysisDataRecord.HookRecord(
                    domain.hook().score(),
                    domain.hook().type(),
                    domain.hook().critique(),
                    domain.hook().suggestion()
            );
        }

        List<TranscriptEntity.AnalysisDataRecord.KeyMomentRecord> keyMoments = domain.keyMoments() == null
                ? List.of()
                : domain.keyMoments().stream()
                        .map(km -> new TranscriptEntity.AnalysisDataRecord.KeyMomentRecord(
                                km.timestamp(), km.title(), km.description()))
                        .toList();

        return new TranscriptEntity.AnalysisDataRecord(
                domain.summary(),
                hookRecord,
                keyMoments,
                domain.takeaways() == null ? List.of() : domain.takeaways()
        );
    }

    private VideoAnalysisData toDomainAnalysis(TranscriptEntity.AnalysisDataRecord entity) {
        if (entity == null) {
            return null;
        }

        Hook hook = null;
        if (entity.hook() != null) {
            hook = new Hook(
                    entity.hook().score(),
                    entity.hook().type(),
                    entity.hook().critique(),
                    entity.hook().suggestion()
            );
        }

        List<KeyMoment> keyMoments = entity.keyMoments() == null
                ? List.of()
                : entity.keyMoments().stream()
                        .map(km -> new KeyMoment(km.timestamp(), km.title(), km.description()))
                        .toList();

        return new VideoAnalysisData(
                entity.summary(),
                hook,
                keyMoments,
                entity.takeaways() == null ? List.of() : entity.takeaways()
        );
    }
}
