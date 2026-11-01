package io.videointel.analysis.infrastructure;

import io.videointel.analysis.application.AnalysisService;
import io.videointel.analysis.domain.AnalysisJob;
import io.videointel.analysis.domain.Hook;
import io.videointel.analysis.domain.IllegalAnalysisStateTransitionException;
import io.videointel.analysis.domain.KeyMoment;
import io.videointel.analysis.domain.Transcript;
import io.videointel.analysis.domain.TranscriptRepositoryPort;
import io.videointel.analysis.domain.TranscriptSegment;
import io.videointel.analysis.domain.VideoAnalysisData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
class AnalysisResultConsumer {

    private final AnalysisService analysisService;
    private final TranscriptRepositoryPort transcriptRepository;

    @RabbitListener(queues = RabbitMqConfig.ANALYSIS_COMPLETED_QUEUE)
    @Transactional
    void handle(AnalysisResultMessage message) {
        // RabbitMQ delivers at-least-once - the same message can arrive twice (e.g. a
        // connection drop between processing and ack). If the job is already past QUEUED,
        // this is a redelivery of something we already handled: log and stop instead of
        // letting the exception propagate, which would just make Spring AMQP requeue the
        // same message forever.
        try {
            analysisService.markRunning(message.jobId());
        } catch (IllegalAnalysisStateTransitionException e) {
            log.warn("Analysis job {} is no longer QUEUED - ignoring duplicate result message", message.jobId());
            return;
        }

        if (message.success()) {
            AnalysisJob job = analysisService.getJob(message.jobId());
            // Same redelivery scenario as above, one step later: if a transcript for this
            // video already exists, this message was already fully processed once and the
            // status change just didn't survive (e.g. a prior attempt in this same method
            // failed after the save and rolled the whole transaction back). Saving again
            // would violate uq_transcripts_video_id and, since that exception propagates,
            // send this message straight back into an infinite requeue loop. We still fall
            // through to markCompleted() below - skipping it would leave the job stuck on
            // RUNNING (already committed by markRunning() above) instead of reflecting that
            // the transcript is in fact there.
            if (transcriptRepository.findByVideoId(job.videoId()).isPresent()) {
                log.warn("Transcript for video {} already exists - skipping duplicate insert", job.videoId());
            } else {
                List<TranscriptSegment> segments = message.segments() == null
                        ? List.of()
                        : message.segments().stream()
                                .map(s -> new TranscriptSegment(s.start(), s.end(), s.text()))
                                .toList();

                VideoAnalysisData analysisData = toDomainAnalysis(message.analysis());

                transcriptRepository.save(Transcript.create(job.videoId(), message.transcriptText(), segments, analysisData));
            }
            analysisService.markCompleted(message.jobId());
        } else {
            analysisService.markFailed(message.jobId(), message.failureReason());
        }
    }

    private VideoAnalysisData toDomainAnalysis(AnalysisResultMessage.AnalysisPayloadDto dto) {
        if (dto == null) {
            return null;
        }

        Hook hook = null;
        if (dto.hook() != null) {
            hook = new Hook(
                    dto.hook().score(),
                    dto.hook().type(),
                    dto.hook().critique(),
                    dto.hook().suggestion()
            );
        }

        List<KeyMoment> keyMoments = dto.keyMoments() == null
                ? List.of()
                : dto.keyMoments().stream()
                        .map(km -> new KeyMoment(km.timestamp(), km.title(), km.description()))
                        .toList();

        return new VideoAnalysisData(
                dto.summary(),
                hook,
                keyMoments,
                dto.takeaways() == null ? List.of() : dto.takeaways()
        );
    }
}
