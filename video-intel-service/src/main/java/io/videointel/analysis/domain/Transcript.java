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
    private final Instant createdAt;

    public static Transcript create(UUID videoId, String text, List<TranscriptSegment> segments) {
        if (videoId == null) {
            throw new IllegalArgumentException("videoId must not be null");
        }
        if (text == null || text.isBlank()) {
            throw new IllegalArgumentException("text must not be blank");
        }
        List<TranscriptSegment> segs = segments == null ? List.of() : List.copyOf(segments);
        return new Transcript(UUID.randomUUID(), videoId, text, segs, Instant.now());
    }

    public static Transcript create(UUID videoId, String text) {
        return create(videoId, text, List.of());
    }

    public static Transcript reconstruct(UUID id, UUID videoId, String text,
                                        List<TranscriptSegment> segments, Instant createdAt) {
        return new Transcript(id, videoId, text, segments == null ? List.of() : List.copyOf(segments), createdAt);
    }
}
