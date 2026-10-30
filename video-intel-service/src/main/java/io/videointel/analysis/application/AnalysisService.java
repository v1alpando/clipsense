package io.videointel.analysis.application;

import io.videointel.analysis.domain.AnalysisJob;
import io.videointel.analysis.domain.AnalysisJobNotFoundException;
import io.videointel.analysis.domain.AnalysisJobRepositoryPort;
import io.videointel.analysis.domain.AnalysisRequestedMessage;
import io.videointel.analysis.domain.OutboxRepositoryPort;
import io.videointel.analysis.domain.UnknownVideoException;
import io.videointel.analysis.domain.VideoNotReadyException;
import io.videointel.video.application.VideoLookupPort;
import io.videointel.video.application.VideoView;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final AnalysisJobRepositoryPort analysisJobRepository;
    private final VideoLookupPort videoLookupPort;
    private final OutboxRepositoryPort outboxRepository;

    @Transactional
    public AnalysisJob requestAnalysis(UUID videoId) {
        // One job per video, ever - a second request just returns the one that's already
        // running/done instead of creating a duplicate (transcripts.video_id is unique, so a
        // second job racing to completion would blow up on save anyway). Retrying a FAILED
        // analysis isn't supported yet - that needs its own decision (new job row vs re-run
        // the same one), not a side effect of this idempotency check.
        Optional<AnalysisJob> existing = analysisJobRepository.findByVideoId(videoId);
        if (existing.isPresent()) {
            return existing.get();
        }

        VideoView videoView = videoLookupPort.findById(videoId)
                .orElseThrow(() -> new UnknownVideoException(videoId));
        if (!videoView.uploaded() || videoView.storageRef() == null) {
            throw new VideoNotReadyException("Video is not uploaded or processing");
        }
        AnalysisJob job = analysisJobRepository.save(AnalysisJob.request(videoId));
        // Written to the outbox table in this same transaction - atomic with the job row above.
        // OutboxRelay picks it up and actually talks to RabbitMQ later, outside this transaction.
        outboxRepository.enqueue(
                AnalysisRequestedMessage.TYPE,
                new AnalysisRequestedMessage(job.id(), job.videoId(), videoView.storageRef())
        );
        return job;
    }

    @Transactional(readOnly = true)
    public AnalysisJob getJob(UUID id) {
        return analysisJobRepository.findById(id).orElseThrow(() -> new AnalysisJobNotFoundException(id));
    }

    @Transactional
    public void markRunning(UUID id) {
        AnalysisJob job = analysisJobRepository.findById(id).orElseThrow(() -> new AnalysisJobNotFoundException(id));
        job.markRunning();
        analysisJobRepository.save(job);
    }

    @Transactional
    public void markCompleted(UUID id) {
        AnalysisJob job = analysisJobRepository.findById(id).orElseThrow(() -> new AnalysisJobNotFoundException(id));
        job.markCompleted();
        analysisJobRepository.save(job);
    }

    @Transactional
    public void markFailed(UUID id, String reason) {
        AnalysisJob job = analysisJobRepository.findById(id).orElseThrow(() -> new AnalysisJobNotFoundException(id));
        job.markFailed(reason);
        analysisJobRepository.save(job);
    }
}
