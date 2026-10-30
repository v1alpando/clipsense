package io.videointel.analysis.domain;

import io.videointel.shared.exception.DomainRuleViolationException;

public class VideoNotReadyException extends DomainRuleViolationException {
    public VideoNotReadyException(String message) {
        super(message);
    }
}
