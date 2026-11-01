package io.videointel.analysis.application;

import io.videointel.analysis.domain.AnalysisDataNotFoundException;
import io.videointel.analysis.domain.AnalysisJobRepositoryPort;
import io.videointel.analysis.domain.Hook;
import io.videointel.analysis.domain.KeyMoment;
import io.videointel.analysis.domain.OutboxRepositoryPort;
import io.videointel.analysis.domain.Transcript;
import io.videointel.analysis.domain.TranscriptNotFoundException;
import io.videointel.analysis.domain.TranscriptRepositoryPort;
import io.videointel.analysis.domain.TranscriptSegment;
import io.videointel.analysis.domain.UnknownVideoException;
import io.videointel.analysis.domain.VideoAccessDeniedException;
import io.videointel.analysis.domain.VideoAnalysisData;
import io.videointel.shared.ownership.Owner;
import io.videointel.video.application.VideoLookupPort;
import io.videointel.video.application.VideoView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalysisServiceTest {

    @Mock
    private AnalysisJobRepositoryPort analysisJobRepository;
    @Mock
    private TranscriptRepositoryPort transcriptRepository;
    @Mock
    private VideoLookupPort videoLookupPort;
    @Mock
    private OutboxRepositoryPort outboxRepository;

    private AnalysisService analysisService;

    @BeforeEach
    void setUp() {
        analysisService = new AnalysisService(
                analysisJobRepository,
                transcriptRepository,
                videoLookupPort,
                outboxRepository
        );
    }

    @Test
    void getTranscript_unknownVideo_throwsException() {
        UUID videoId = UUID.randomUUID();
        Owner requester = new Owner.RegisteredUser(UUID.randomUUID());
        when(videoLookupPort.findById(videoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.getTranscript(videoId, requester))
                .isInstanceOf(UnknownVideoException.class);
    }

    @Test
    void getTranscript_differentOwner_throwsAccessDenied() {
        UUID videoId = UUID.randomUUID();
        Owner actualOwner = new Owner.RegisteredUser(UUID.randomUUID());
        Owner requester = new Owner.RegisteredUser(UUID.randomUUID());

        when(videoLookupPort.findById(videoId)).thenReturn(Optional.of(
                new VideoView(videoId, true, "storage-ref", actualOwner)
        ));

        assertThatThrownBy(() -> analysisService.getTranscript(videoId, requester))
                .isInstanceOf(VideoAccessDeniedException.class);
    }

    @Test
    void getTranscript_transcriptNotFound_throwsNotFoundException() {
        UUID videoId = UUID.randomUUID();
        Owner owner = new Owner.RegisteredUser(UUID.randomUUID());

        when(videoLookupPort.findById(videoId)).thenReturn(Optional.of(
                new VideoView(videoId, true, "storage-ref", owner)
        ));
        when(transcriptRepository.findByVideoId(videoId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> analysisService.getTranscript(videoId, owner))
                .isInstanceOf(TranscriptNotFoundException.class);
    }

    @Test
    void getTranscript_validOwnerAndTranscriptExists_returnsTranscript() {
        UUID videoId = UUID.randomUUID();
        Owner owner = new Owner.RegisteredUser(UUID.randomUUID());
        Transcript transcript = Transcript.create(videoId, "Sample text", List.of(
                new TranscriptSegment(0.0, 2.0, "Sample text")
        ));

        when(videoLookupPort.findById(videoId)).thenReturn(Optional.of(
                new VideoView(videoId, true, "storage-ref", owner)
        ));
        when(transcriptRepository.findByVideoId(videoId)).thenReturn(Optional.of(transcript));

        Transcript result = analysisService.getTranscript(videoId, owner);

        assertThat(result).isNotNull();
        assertThat(result.text()).isEqualTo("Sample text");
        assertThat(result.segments()).hasSize(1);
        assertThat(result.segments().get(0).text()).isEqualTo("Sample text");
    }

    @Test
    void getAnalysisData_analysisDataPresent_returnsData() {
        UUID videoId = UUID.randomUUID();
        Owner owner = new Owner.RegisteredUser(UUID.randomUUID());
        VideoAnalysisData analysisData = new VideoAnalysisData(
                "Great video",
                new Hook(95, "CURIOSITY", "Excellent", "None"),
                List.of(new KeyMoment(10.0, "Climax", "Big reveal")),
                List.of("Insight 1")
        );
        Transcript transcript = Transcript.create(
                videoId, "Sample text",
                List.of(new TranscriptSegment(0.0, 2.0, "Sample text")),
                analysisData
        );

        when(videoLookupPort.findById(videoId)).thenReturn(Optional.of(
                new VideoView(videoId, true, "storage-ref", owner)
        ));
        when(transcriptRepository.findByVideoId(videoId)).thenReturn(Optional.of(transcript));

        VideoAnalysisData result = analysisService.getAnalysisData(videoId, owner);

        assertThat(result).isNotNull();
        assertThat(result.summary()).isEqualTo("Great video");
        assertThat(result.hook().score()).isEqualTo(95);
        assertThat(result.keyMoments()).hasSize(1);
        assertThat(result.takeaways()).containsExactly("Insight 1");
    }

    @Test
    void getAnalysisData_analysisDataNull_throwsNotFound() {
        UUID videoId = UUID.randomUUID();
        Owner owner = new Owner.RegisteredUser(UUID.randomUUID());
        Transcript transcript = Transcript.create(
                videoId, "Sample text",
                List.of(new TranscriptSegment(0.0, 2.0, "Sample text")),
                null
        );

        when(videoLookupPort.findById(videoId)).thenReturn(Optional.of(
                new VideoView(videoId, true, "storage-ref", owner)
        ));
        when(transcriptRepository.findByVideoId(videoId)).thenReturn(Optional.of(transcript));

        assertThatThrownBy(() -> analysisService.getAnalysisData(videoId, owner))
                .isInstanceOf(AnalysisDataNotFoundException.class);
    }
}
