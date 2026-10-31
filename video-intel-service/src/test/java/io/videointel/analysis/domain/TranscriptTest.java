package io.videointel.analysis.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranscriptTest {

    @Test
    void create_validInput_createsTranscriptWithSegments() {
        UUID videoId = UUID.randomUUID();
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0.0, 5.2, "Hello world"),
                new TranscriptSegment(5.2, 10.0, "Welcome to the video")
        );

        Transcript transcript = Transcript.create(videoId, "Hello world. Welcome to the video", segments);

        assertThat(transcript.id()).isNotNull();
        assertThat(transcript.videoId()).isEqualTo(videoId);
        assertThat(transcript.text()).isEqualTo("Hello world. Welcome to the video");
        assertThat(transcript.segments()).hasSize(2);
        assertThat(transcript.segments().get(0).start()).isEqualTo(0.0);
        assertThat(transcript.segments().get(0).end()).isEqualTo(5.2);
        assertThat(transcript.segments().get(0).text()).isEqualTo("Hello world");
        assertThat(transcript.createdAt()).isNotNull();
    }

    @Test
    void create_nullSegments_defaultsToEmptyList() {
        UUID videoId = UUID.randomUUID();
        Transcript transcript = Transcript.create(videoId, "Some text", null);

        assertThat(transcript.segments()).isNotNull().isEmpty();
    }

    @Test
    void create_nullVideoId_throwsException() {
        assertThatThrownBy(() -> Transcript.create(null, "Text", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("videoId must not be null");
    }

    @Test
    void create_blankText_throwsException() {
        UUID videoId = UUID.randomUUID();
        assertThatThrownBy(() -> Transcript.create(videoId, "   ", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("text must not be blank");
    }

    @Test
    void reconstruct_preservesAllFields() {
        UUID id = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T12:00:00Z");
        List<TranscriptSegment> segments = List.of(new TranscriptSegment(1.0, 2.5, "Part"));

        Transcript transcript = Transcript.reconstruct(id, videoId, "Part", segments, createdAt);

        assertThat(transcript.id()).isEqualTo(id);
        assertThat(transcript.videoId()).isEqualTo(videoId);
        assertThat(transcript.text()).isEqualTo("Part");
        assertThat(transcript.segments()).isEqualTo(segments);
        assertThat(transcript.createdAt()).isEqualTo(createdAt);
    }
}
