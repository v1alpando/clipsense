package io.videointel.chat.infrastructure;

import io.videointel.chat.domain.ChatMessage;
import io.videointel.chat.domain.ChatMessageRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class ChatMessageRepositoryAdapter implements ChatMessageRepositoryPort {

    private final ChatMessageRepository jpaRepository;

    @Override
    public ChatMessage save(ChatMessage message) {
        ChatMessageEntity entity = new ChatMessageEntity(
                message.id(), message.sessionId(), message.role(), message.content(), message.createdAt()
        );
        jpaRepository.save(entity);
        return message;
    }

    @Override
    public List<ChatMessage> findBySessionId(UUID sessionId) {
        return jpaRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream().map(this::toDomain).toList();
    }

    private ChatMessage toDomain(ChatMessageEntity entity) {
        return ChatMessage.reconstruct(
                entity.getId(), entity.getSessionId(), entity.getRole(), entity.getContent(), entity.getCreatedAt()
        );
    }
}
