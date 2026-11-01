package io.videointel.analysis.api;

import io.videointel.analysis.application.AnalysisService;
import io.videointel.analysis.domain.Hook;
import io.videointel.analysis.domain.KeyMoment;
import io.videointel.analysis.domain.Transcript;
import io.videointel.analysis.domain.TranscriptSegment;
import io.videointel.analysis.domain.VideoAccessDeniedException;
import io.videointel.analysis.domain.VideoAnalysisData;
import io.videointel.shared.ownership.Owner;
import io.videointel.shared.ownership.OwnerType;
import io.videointel.shared.security.MissingOwnerException;
import io.videointel.shared.security.OwnerExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisControllerTest {

    @Mock
    private AnalysisService analysisService;
    @Mock
    private OwnerExtractor ownerExtractor;

    private AnalysisController controller;

    @BeforeEach
    void setUp() {
        controller = new AnalysisController(analysisService, ownerExtractor);
    }

    @Test
    void getTranscript_withValidOwner_returnsFullResponse() {
        UUID videoId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Owner owner = Owner.of(OwnerType.USER, ownerId);

        when(ownerExtractor.extract(ownerId, OwnerType.USER)).thenReturn(Optional.of(owner));

        VideoAnalysisData analysisData = new VideoAnalysisData(
                "Short summary",
                new Hook(80, "QUESTION", "Good", "Refine ending"),
                List.of(new KeyMoment(5.0, "Key moment 1", "Description")),
                List.of("Takeaway 1")
        );
        Transcript transcript = Transcript.create(
                videoId, "Hello world",
                List.of(new TranscriptSegment(0.0, 5.0, "Hello world")),
                analysisData
        );

        when(analysisService.getTranscript(videoId, owner)).thenReturn(transcript);

        TranscriptResponse response = controller.getTranscript(videoId, ownerId, OwnerType.USER, null, null);

        assertThat(response.videoId()).isEqualTo(videoId);
        assertThat(response.fullText()).isEqualTo("Hello world");
        assertThat(response.text()).isEqualTo("Hello world");
        assertThat(response.segments()).hasSize(1);
        assertThat(response.analysis()).isNotNull();
        assertThat(response.analysis().summary()).isEqualTo("Short summary");
        assertThat(response.analysis().hook().score()).isEqualTo(80);
        assertThat(response.analysis().hook().type()).isEqualTo("QUESTION");
        assertThat(response.analysis().hook().hookType()).isEqualTo("QUESTION");
        assertThat(response.analysis().keyMoments()).hasSize(1);
        assertThat(response.analysis().keyMoments().get(0).title()).isEqualTo("Key moment 1");
    }

    @Test
    void getTranscript_withoutOwner_returnsTranscriptForPublicPlayback() {
        UUID videoId = UUID.randomUUID();
        Transcript transcript = Transcript.reconstruct(UUID.randomUUID(), videoId, "Hello world", List.of(), null, Instant.now());

        when(ownerExtractor.extract(null, null)).thenReturn(Optional.empty());
        when(analysisService.getTranscript(videoId, null)).thenReturn(transcript);

        TranscriptResponse response = controller.getTranscript(videoId, null, null, null, null);

        assertThat(response).isNotNull();
        assertThat(response.videoId()).isEqualTo(videoId);
        assertThat(response.text()).isEqualTo("Hello world");
    }

    @Test
    void getAnalysisData_withValidOwner_returnsAnalysisResponse() {
        UUID videoId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Owner owner = Owner.of(OwnerType.ANONYMOUS_SESSION, ownerId);

        when(ownerExtractor.extract(ownerId, OwnerType.ANONYMOUS_SESSION)).thenReturn(Optional.of(owner));

        VideoAnalysisData analysisData = new VideoAnalysisData(
                "AI summary",
                new Hook(90, "STATEMENT", "Punchy", "None"),
                List.of(new KeyMoment(12.0, "Crucial point", "Explained")),
                List.of("Insight A")
        );

        when(analysisService.getAnalysisData(videoId, owner)).thenReturn(analysisData);

        TranscriptResponse.AnalysisResponse response = controller.getAnalysisData(
                videoId, ownerId, OwnerType.ANONYMOUS_SESSION, null, null
        );

        assertThat(response.summary()).isEqualTo("AI summary");
        assertThat(response.hook().score()).isEqualTo(90);
        assertThat(response.keyMoments()).hasSize(1);
        assertThat(response.takeaways()).containsExactly("Insight A");
    }

    @Test
    void getInsights_withValidOwner_returnsVideoInsightsResponse() {
        UUID videoId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Owner owner = Owner.of(OwnerType.USER, ownerId);

        when(ownerExtractor.extract(ownerId, OwnerType.USER)).thenReturn(Optional.of(owner));

        VideoAnalysisData analysisData = new VideoAnalysisData(
                "AI summary",
                new Hook(90, "STATEMENT", "Punchy", "None"),
                List.of(new KeyMoment(12.0, "Crucial point", "Explained")),
                List.of("Insight A")
        );

        when(analysisService.getAnalysisData(videoId, owner)).thenReturn(analysisData);

        VideoInsightsResponse response = controller.getInsights(videoId, ownerId, OwnerType.USER, null, null);

        assertThat(response.videoId()).isEqualTo(videoId);
        assertThat(response.summary().tldr()).isEqualTo("AI summary");
        assertThat(response.summary().takeaways()).containsExactly("Insight A");
        assertThat(response.hook().score()).isEqualTo(90);
        assertThat(response.keyMoments().get(0).id()).isNotNull();
    }
}
