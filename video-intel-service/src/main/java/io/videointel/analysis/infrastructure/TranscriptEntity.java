package io.videointel.analysis.infrastructure;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "transcripts", schema = "analysis")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class TranscriptEntity implements Persistable<UUID> {

    @Id
    private UUID id;

    @Column(name = "video_id", nullable = false, unique = true)
    private UUID videoId;

    @Column(nullable = false, columnDefinition = "text")
    private String text;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "segments", columnDefinition = "jsonb")
    private List<SegmentRecord> segments;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "analysis_data", columnDefinition = "jsonb")
    private AnalysisDataRecord analysisData;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    TranscriptEntity(UUID id, UUID videoId, String text, List<SegmentRecord> segments,
                     AnalysisDataRecord analysisData, Instant createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.text = text;
        this.segments = segments;
        this.analysisData = analysisData;
        this.createdAt = createdAt;
    }

    TranscriptEntity(UUID id, UUID videoId, String text, List<SegmentRecord> segments, Instant createdAt) {
        this(id, videoId, text, segments, null, createdAt);
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }

    record SegmentRecord(double start, double end, String text) {
    }

    record AnalysisDataRecord(
            String summary,
            HookRecord hook,
            List<KeyMomentRecord> keyMoments,
            List<String> takeaways
    ) {
        record HookRecord(int score, String type, String critique, String suggestion) {
        }

        record KeyMomentRecord(double timestamp, String title, String description) {
        }
    }
}
