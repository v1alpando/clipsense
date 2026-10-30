package io.videointel.user.domain;

import io.videointel.shared.exception.NotFoundException;

import java.util.UUID;

public class UserNotFoundException extends NotFoundException {

    public UserNotFoundException(UUID id) {
        super("User not found: " + id);
    }
}
