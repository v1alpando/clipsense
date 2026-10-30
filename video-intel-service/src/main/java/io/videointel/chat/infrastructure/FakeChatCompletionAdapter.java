package io.videointel.chat.infrastructure;

import io.videointel.chat.domain.ChatCompletionPort;
import io.videointel.chat.domain.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TEMPORARY stand-in for a real LLM call (Claude/OpenAI). ChatService only knows about
 * ChatCompletionPort, so swapping this for a real provider later means writing one new
 * adapter class - nothing else in this module changes.
 */
@Component
class FakeChatCompletionAdapter implements ChatCompletionPort {

    @Override
    public String generateReply(String transcriptText, List<ChatMessage> history, String userMessage) {
        return "This is a fake reply. You asked: \"" + userMessage + "\". "
                + "The video transcript is " + transcriptText.length() + " characters long, "
                + "and this session has " + history.size() + " message(s) so far.";
    }
}
