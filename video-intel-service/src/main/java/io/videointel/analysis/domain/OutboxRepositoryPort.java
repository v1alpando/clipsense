package io.videointel.analysis.domain;

import java.util.List;
import java.util.UUID;

public interface OutboxRepositoryPort {

    /**
     * Serializes payload and writes it as an unprocessed row, in whatever transaction is
     * currently active on the caller's thread - that's the whole point of the outbox: this
     * write must land atomically with the business data change it's reporting on.
     */
    void enqueue(String messageType, Object payload);

    List<OutboxMessage> findUnprocessed();

    void markProcessed(UUID id);
}
