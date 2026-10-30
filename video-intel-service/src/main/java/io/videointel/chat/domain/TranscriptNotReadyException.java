package io.videointel.chat.domain;

import io.videointel.shared.exception.DomainRuleViolationException;

import java.util.UUID;

public class TranscriptNotReadyException extends DomainRuleViolationException {

    public TranscriptNotReadyException(UUID videoId) {
        super("Transcript not ready for video: " + videoId);
    }
}
