package io.videointel.shared.exception;

public abstract class DomainRuleViolationException extends RuntimeException {

    protected DomainRuleViolationException(String message) {
        super(message);
    }
}
