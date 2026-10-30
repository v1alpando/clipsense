package io.videointel.analysis.infrastructure;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.videointel.analysis.domain.OutboxMessage;
import io.videointel.analysis.domain.OutboxRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
class OutboxRepositoryAdapter implements OutboxRepositoryPort {

    private final OutboxRepository jpaRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void enqueue(String messageType, Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize outbox payload of type " + messageType, e);
        }

        OutboxMessageEntity entity = new OutboxMessageEntity();
        entity.setId(UUID.randomUUID());
        entity.setMessageType(messageType);
        entity.setPayload(json);
        entity.setCreatedAt(Instant.now());
        jpaRepository.save(entity);
    }

    @Override
    public List<OutboxMessage> findUnprocessed() {
        return jpaRepository.findByProcessedAtIsNull().stream().map(this::toDomain).toList();
    }

    @Override
    public void markProcessed(UUID id) {
        OutboxMessageEntity entity = jpaRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Unknown outbox message: " + id));
        entity.setProcessedAt(Instant.now());
        jpaRepository.save(entity);
    }

    private OutboxMessage toDomain(OutboxMessageEntity entity) {
        return OutboxMessage.reconstruct(
                entity.getId(), entity.getMessageType(), entity.getPayload(),
                entity.getCreatedAt(), entity.getProcessedAt()
        );
    }
}
