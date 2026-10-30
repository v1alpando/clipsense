package io.videointel.analysis.infrastructure;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class RabbitMqConfig {

    // Package-visible on purpose: these two queue names are the actual contract between
    // this service and whatever consumes/produces on them (the fake worker today, a real
    // Python worker later). Every class that needs a queue name references these constants
    // instead of repeating the literal string.
    static final String ANALYSIS_REQUESTED_QUEUE = "analysis.requested";
    static final String ANALYSIS_COMPLETED_QUEUE = "analysis.completed";

    @Bean
    Queue analysisRequestedQueue() {
        return new Queue(ANALYSIS_REQUESTED_QUEUE, true);
    }

    @Bean
    Queue analysisCompletedQueue() {
        return new Queue(ANALYSIS_COMPLETED_QUEUE, true);
    }

    // Spring Boot's own auto-configured ObjectMapper is Jackson 3 (tools.jackson.*) as of
    // Boot 4.1, but Spring AMQP's Jackson2JsonMessageConverter - and our own outbox
    // serialization - are built against classic Jackson 2 (com.fasterxml.jackson.databind).
    // Declaring this bean explicitly is what makes a Jackson-2 ObjectMapper available at all;
    // OutboxRepositoryAdapter/OutboxRelay are wired to this exact bean, not Boot's default one.
    @Bean
    ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    MessageConverter messageConverter(ObjectMapper objectMapper) {
        // JSON, not Java's default binary serialization - the whole point of this queue is
        // that a non-Java (Python) worker will eventually read from it.
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
