package io.videointel.analysis.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.videointel.analysis.domain.AnalysisRequestedMessage;
import io.videointel.analysis.domain.OutboxMessage;
import io.videointel.analysis.domain.OutboxRepositoryPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Reads rows that {@link OutboxRepositoryPort#enqueue} wrote atomically alongside business data,
 * and actually publishes them to RabbitMQ - the only component in this module that's allowed to
 * fail without losing anything: an unprocessed row just gets picked up again on the next tick.
 */
@Slf4j
@Component
@RequiredArgsConstructor
class OutboxRelay {

    private final OutboxRepositoryPort outboxRepository;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.SECONDS)
    void relay() {
        List<OutboxMessage> unprocessed = outboxRepository.findUnprocessed();

        for (OutboxMessage message : unprocessed) {
            try {
                publish(message);
                outboxRepository.markProcessed(message.id());
            } catch (Exception e) {
                log.warn("Failed to relay outbox message {} ({}), will retry next tick",
                        message.id(), message.messageType(), e);
            }
        }
    }

    private void publish(OutboxMessage message) throws Exception {
        switch (message.messageType()) {
            case AnalysisRequestedMessage.TYPE -> rabbitTemplate.convertAndSend(
                    RabbitMqConfig.ANALYSIS_REQUESTED_QUEUE,
                    objectMapper.readValue(message.payload(), AnalysisRequestedMessage.class)
            );
            default -> throw new IllegalStateException("Unknown outbox message type: " + message.messageType());
        }
    }
}
