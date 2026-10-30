package io.videointel.user.infrastructure;

import io.videointel.user.domain.AnonymousSession;
import io.videointel.user.domain.AnonymousSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class AnonymousSessionRepositoryAdapter implements AnonymousSessionRepositoryPort {

    private final AnonymousSessionRepository jpaRepository;

    @Override
    public AnonymousSession save(AnonymousSession session) {
        AnonymousSessionEntity entity = new AnonymousSessionEntity(
                session.id(), session.createdAt(), session.expiresAt()
        );
        jpaRepository.save(entity);
        return session;
    }

    @Override
    public Optional<AnonymousSession> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    @Override
    public boolean existsById(UUID id) {
        return jpaRepository.existsById(id);
    }

    private AnonymousSession toDomain(AnonymousSessionEntity entity) {
        return AnonymousSession.reconstruct(entity.getId(), entity.getCreatedAt(), entity.getExpiresAt());
    }
}
