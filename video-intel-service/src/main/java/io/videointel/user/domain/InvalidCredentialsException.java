package io.videointel.user.domain;

import io.videointel.shared.exception.UnauthorizedException;

public class InvalidCredentialsException extends UnauthorizedException {

    public InvalidCredentialsException() {
        // Deliberately generic - never say which one (email vs password) was wrong.
        // Doing otherwise lets an attacker enumerate which emails are registered.
        super("Invalid email or password");
    }
}
