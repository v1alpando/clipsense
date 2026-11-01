package io.videointel.analysis.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class Transcript {

    private final UUID id;
    private final UUID videoId;
    private final String text;
    private final List<TranscriptSegment> segments;
    private final VideoAnalysisData analysisData;
    private final Instant createdAt;

    public static Transcript create(UUID videoId, String text, List<TranscriptSegment> segments, VideoAnalysisData analysisData) {
        if (videoId == null) {
            throw new IllegalArgumentException("videoId must not be null");
        }
        String effectiveText = text == null ? "" : text;
        List<TranscriptSegment> segs = segments == null ? List.of() : List.copyOf(segments);
        return new Transcript(UUID.randomUUID(), videoId, effectiveText, segs, analysisData, Instant.now());
    }

    public static Transcript create(UUID videoId, String text, List<TranscriptSegment> segments) {
        return create(videoId, text, segments, null);
    }

    public static Transcript create(UUID videoId, String text) {
        return create(videoId, text, List.of(), null);
    }

    public static Transcript reconstruct(UUID id, UUID videoId, String text,
                                        List<TranscriptSegment> segments,
                                        VideoAnalysisData analysisData,
                                        Instant createdAt) {
        return new Transcript(id, videoId, text == null ? "" : text,
                segments == null ? List.of() : List.copyOf(segments),
                analysisData, createdAt);
    }

    public static Transcript reconstruct(UUID id, UUID videoId, String text,
                                        List<TranscriptSegment> segments,
                                        Instant createdAt) {
        return reconstruct(id, videoId, text, segments, null, createdAt);
    }
}
