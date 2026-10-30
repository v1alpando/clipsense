package io.videointel.chat.domain;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepositoryPort {

    ChatMessage save(ChatMessage message);

    List<ChatMessage> findBySessionId(UUID sessionId);
}
