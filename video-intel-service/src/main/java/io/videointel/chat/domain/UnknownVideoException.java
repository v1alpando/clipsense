package io.videointel.chat.domain;

import io.videointel.shared.exception.DomainRuleViolationException;

import java.util.UUID;

public class UnknownVideoException extends DomainRuleViolationException {

    public UnknownVideoException(UUID id) {
        super("Video not found: " + id);
    }
}
