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
import org.springframework.data.domain.Persistable;

import java.time.Instant;
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

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Transient
    private boolean isNew = true;

    TranscriptEntity(UUID id, UUID videoId, String text, Instant createdAt) {
        this.id = id;
        this.videoId = videoId;
        this.text = text;
        this.createdAt = createdAt;
    }

    @PostLoad
    @PostPersist
    void markNotNew() {
        this.isNew = false;
    }
}
