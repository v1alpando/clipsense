package io.videointel.shared.ownership;

import java.util.UUID;

/**
 * A resource owner is always one of two kinds: a registered user, or a browser/device-scoped
 * anonymous session. Lives here (not in `video.domain`) because both `video` and `user` need it -
 * `user.application.OwnerLookupPort` accepts one, `video.domain.Video` stores one. Neither module
 * owns the other's domain type this way. Adding a third kind later (e.g. a service/API-key
 * principal) means adding one more record here - callers that already pattern-match exhaustively
 * on Owner will get a compile error until they handle it too.
 */
public sealed interface Owner {

    UUID id();

    OwnerType type();

    record RegisteredUser(UUID id) implements Owner {
        @Override
        public OwnerType type() {
            return OwnerType.USER;
        }
    }

    record AnonymousSession(UUID id) implements Owner {
        @Override
        public OwnerType type() {
            return OwnerType.ANONYMOUS_SESSION;
        }
    }

    static Owner of(OwnerType type, UUID id) {
        return switch (type) {
            case USER -> new RegisteredUser(id);
            case ANONYMOUS_SESSION -> new AnonymousSession(id);
        };
    }
}
