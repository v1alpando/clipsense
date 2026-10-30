package io.videointel.chat.infrastructure;

import io.videointel.chat.domain.ChatSession;
import io.videointel.chat.domain.ChatSessionRepositoryPort;
import io.videointel.shared.ownership.Owner;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class ChatSessionRepositoryAdapter implements ChatSessionRepositoryPort {

    private final ChatSessionRepository jpaRepository;

    @Override
    public ChatSession save(ChatSession session) {
        ChatSessionEntity entity = new ChatSessionEntity(
                session.id(), session.videoId(), session.owner().type(), session.owner().id(), session.createdAt()
        );
        jpaRepository.save(entity);
        return session;
    }

    @Override
    public Optional<ChatSession> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }

    private ChatSession toDomain(ChatSessionEntity entity) {
        Owner owner = Owner.of(entity.getOwnerType(), entity.getOwnerId());
        return ChatSession.reconstruct(entity.getId(), entity.getVideoId(), owner, entity.getCreatedAt());
    }
}
