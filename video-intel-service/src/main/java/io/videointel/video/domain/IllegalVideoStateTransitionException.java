package io.videointel.video.domain;

import io.videointel.shared.exception.DomainRuleViolationException;

public class IllegalVideoStateTransitionException extends DomainRuleViolationException {
    public IllegalVideoStateTransitionException(String message) {
        super(message);
    }
}
