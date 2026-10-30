package io.videointel.chat.domain;

import io.videointel.shared.exception.DomainRuleViolationException;

import java.util.UUID;

public class UnknownOwnerException extends DomainRuleViolationException {

    public UnknownOwnerException(UUID id) {
        super("Owner not found: " + id);
    }
}
