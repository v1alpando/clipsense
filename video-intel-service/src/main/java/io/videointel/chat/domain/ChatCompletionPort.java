package io.videointel.chat.domain;

import java.util.List;

public interface ChatCompletionPort {

    String generateReply(String transcriptText, List<ChatMessage> history, String userMessage);
}
