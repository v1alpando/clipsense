package io.videointel.analysis.application;

import io.videointel.analysis.domain.Transcript;
import io.videointel.analysis.domain.TranscriptRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class TranscriptLookupService implements TranscriptLookupPort {

    private final TranscriptRepositoryPort transcriptRepository;

    @Override
    public Optional<String> findTranscriptText(UUID videoId) {
        return transcriptRepository.findByVideoId(videoId).map(Transcript::text);
    }
}
