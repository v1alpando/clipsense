package io.videointel.shared.security;

import io.videointel.shared.exception.UnauthorizedException;

public class MissingOwnerException extends UnauthorizedException {

    public MissingOwnerException() {
        super("Authentication or owner credentials (X-Owner-Id / X-Owner-Type) required");
    }
}
