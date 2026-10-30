package io.videointel.chat.domain;

import java.util.Optional;
import java.util.UUID;

public interface ChatSessionRepositoryPort {

    ChatSession save(ChatSession session);

    Optional<ChatSession> findById(UUID id);
}
