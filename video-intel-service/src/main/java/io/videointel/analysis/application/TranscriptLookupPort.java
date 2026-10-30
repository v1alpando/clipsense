package io.videointel.analysis.application;

import java.util.Optional;
import java.util.UUID;

public interface TranscriptLookupPort {

    Optional<String> findTranscriptText(UUID videoId);
}
