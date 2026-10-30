package io.videointel.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface TokenIssuerPort {

    String issueToken(UUID userId, String email);

    /**
     * Verifies signature and expiry, and returns the user id it was issued for.
     * Empty means "not authenticated" - the filter decides what to do with that, not this port.
     */
    Optional<UUID> verify(String token);
}
