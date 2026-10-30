package io.videointel.user.domain;

import io.videointel.shared.exception.NotFoundException;

import java.util.UUID;

public class AnonymousSessionNotFoundException extends NotFoundException {

    public AnonymousSessionNotFoundException(UUID id) {
        super("Anonymous session not found: " + id);
    }
}
