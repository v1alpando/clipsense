package io.videointel.analysis.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TranscriptTest {

    @Test
    void create_validInput_createsTranscriptWithSegmentsAndAnalysis() {
        UUID videoId = UUID.randomUUID();
        List<TranscriptSegment> segments = List.of(
                new TranscriptSegment(0.0, 5.2, "Hello world"),
                new TranscriptSegment(5.2, 10.0, "Welcome to the video")
        );
        VideoAnalysisData analysisData = new VideoAnalysisData(
                "Short summary of the video",
                new Hook(85, "QUESTION", "Good hook", "Add more intrigue"),
                List.of(new KeyMoment(2.5, "Intro", "The speaker explains the core idea")),
                List.of("Takeaway 1", "Takeaway 2")
        );

        Transcript transcript = Transcript.create(videoId, "Hello world. Welcome to the video", segments, analysisData);

        assertThat(transcript.id()).isNotNull();
        assertThat(transcript.videoId()).isEqualTo(videoId);
        assertThat(transcript.text()).isEqualTo("Hello world. Welcome to the video");
        assertThat(transcript.segments()).hasSize(2);
        assertThat(transcript.segments().get(0).start()).isEqualTo(0.0);
        assertThat(transcript.segments().get(0).end()).isEqualTo(5.2);
        assertThat(transcript.segments().get(0).text()).isEqualTo("Hello world");
        assertThat(transcript.analysisData()).isNotNull();
        assertThat(transcript.analysisData().summary()).isEqualTo("Short summary of the video");
        assertThat(transcript.analysisData().hook().score()).isEqualTo(85);
        assertThat(transcript.analysisData().hook().type()).isEqualTo("QUESTION");
        assertThat(transcript.analysisData().keyMoments()).hasSize(1);
        assertThat(transcript.analysisData().keyMoments().get(0).title()).isEqualTo("Intro");
        assertThat(transcript.analysisData().takeaways()).containsExactly("Takeaway 1", "Takeaway 2");
        assertThat(transcript.createdAt()).isNotNull();
    }

    @Test
    void create_nullSegments_defaultsToEmptyList() {
        UUID videoId = UUID.randomUUID();
        Transcript transcript = Transcript.create(videoId, "Some text", null);

        assertThat(transcript.segments()).isNotNull().isEmpty();
        assertThat(transcript.analysisData()).isNull();
    }

    @Test
    void create_nullVideoId_throwsException() {
        assertThatThrownBy(() -> Transcript.create(null, "Text", List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("videoId must not be null");
    }

    @Test
    void create_blankText_allowsEmptyTranscript() {
        UUID videoId = UUID.randomUUID();
        Transcript transcript = Transcript.create(videoId, "   ", List.of());
        assertThat(transcript.text()).isEqualTo("   ");
    }

    @Test
    void create_nullText_defaultsToEmptyString() {
        UUID videoId = UUID.randomUUID();
        Transcript transcript = Transcript.create(videoId, null, List.of());
        assertThat(transcript.text()).isEqualTo("");
    }

    @Test
    void reconstruct_preservesAllFieldsIncludingAnalysis() {
        UUID id = UUID.randomUUID();
        UUID videoId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-01T12:00:00Z");
        List<TranscriptSegment> segments = List.of(new TranscriptSegment(1.0, 2.5, "Part"));
        VideoAnalysisData analysisData = new VideoAnalysisData(
                "Summary",
                new Hook(90, "STATEMENT", "Strong", "None"),
                List.of(new KeyMoment(1.0, "Title", "Desc")),
                List.of("T1")
        );

        Transcript transcript = Transcript.reconstruct(id, videoId, "Part", segments, analysisData, createdAt);

        assertThat(transcript.id()).isEqualTo(id);
        assertThat(transcript.videoId()).isEqualTo(videoId);
        assertThat(transcript.text()).isEqualTo("Part");
        assertThat(transcript.segments()).isEqualTo(segments);
        assertThat(transcript.analysisData()).isEqualTo(analysisData);
        assertThat(transcript.createdAt()).isEqualTo(createdAt);
    }
}
