package io.videointel.video.application;

import io.videointel.video.domain.Video;
import io.videointel.video.domain.VideoRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
class StaleUploadReconciler {

    private static final Duration STALE_AFTER = Duration.ofMinutes(30);

    private final VideoRepositoryPort videoRepository;
    private final VideoService videoService;

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    void reconcileStaleUploads() {
        Instant threshold = Instant.now().minus(STALE_AFTER);
        List<Video> stale = videoRepository.findStaleUploading(threshold);

        for (Video video : stale) {
            try {
                videoService.completeUpload(video.id());
            } catch (Exception e) {
                log.warn("Failed to reconcile stale upload for video {}", video.id(), e);
            }
        }

        if (!stale.isEmpty()) {
            log.info("Reconciled {} stale upload(s)", stale.size());
        }
    }
}
