package io.videointel.analysis.domain;

import io.videointel.shared.exception.DomainRuleViolationException;

public class IllegalAnalysisStateTransitionException extends DomainRuleViolationException {
    public IllegalAnalysisStateTransitionException(String message) {
        super(message);
    }
}
