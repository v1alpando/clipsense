package io.videointel.user.domain;

import io.videointel.shared.exception.DomainRuleViolationException;

public class EmailAlreadyRegisteredException extends DomainRuleViolationException {

    public EmailAlreadyRegisteredException(String email) {
        super("Email already registered: " + email);
    }
}
