package io.videointel.user.domain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.time.Instant;
import java.util.UUID;

@Getter
@Accessors(fluent = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public final class User {

    private final UUID id;
    private final String email;
    private final String passwordHash;
    private final Instant createdAt;

    /**
     * Takes an already-hashed password, not a raw one - hashing needs a specific algorithm
     * and library (BCrypt via Spring Security's PasswordEncoder), which is an infrastructure
     * concern, not a domain one. The caller (AuthService) hashes before calling this.
     */
    public static User register(String email, String passwordHash) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("passwordHash must not be blank");
        }
        return new User(UUID.randomUUID(), email, passwordHash, Instant.now());
    }

    public static User reconstruct(UUID id, String email, String passwordHash, Instant createdAt) {
        return new User(id, email, passwordHash, createdAt);
    }
}
