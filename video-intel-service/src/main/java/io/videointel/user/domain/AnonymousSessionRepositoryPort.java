package io.videointel.user.domain;

import java.util.Optional;
import java.util.UUID;

public interface AnonymousSessionRepositoryPort {

    AnonymousSession save(AnonymousSession session);

    Optional<AnonymousSession> findById(UUID id);

    boolean existsById(UUID id);
}
