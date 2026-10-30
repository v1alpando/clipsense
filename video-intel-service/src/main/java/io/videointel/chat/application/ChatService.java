package io.videointel.chat.application;

import io.videointel.analysis.application.TranscriptLookupPort;
import io.videointel.chat.domain.ChatCompletionPort;
import io.videointel.chat.domain.ChatMessage;
import io.videointel.chat.domain.ChatMessageRepositoryPort;
import io.videointel.chat.domain.ChatSession;
import io.videointel.chat.domain.ChatSessionNotFoundException;
import io.videointel.chat.domain.ChatSessionRepositoryPort;
import io.videointel.chat.domain.MessageRole;
import io.videointel.chat.domain.TranscriptNotReadyException;
import io.videointel.chat.domain.UnknownOwnerException;
import io.videointel.chat.domain.UnknownVideoException;
import io.videointel.shared.ownership.Owner;
import io.videointel.user.application.OwnerLookupPort;
import io.videointel.video.application.VideoLookupPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatSessionRepositoryPort chatSessionRepository;
    private final ChatMessageRepositoryPort chatMessageRepository;
    private final OwnerLookupPort ownerLookupPort;
    private final VideoLookupPort videoLookupPort;
    private final TranscriptLookupPort transcriptLookupPort;
    private final ChatCompletionPort chatCompletionPort;

    @Transactional
    public ChatSession createSession(UUID videoId, Owner owner) {
        if (!ownerLookupPort.exists(owner)) {
            throw new UnknownOwnerException(owner.id());
        }
        if (videoLookupPort.findById(videoId).isEmpty()) {
            throw new UnknownVideoException(videoId);
        }
        return chatSessionRepository.save(ChatSession.start(videoId, owner));
    }

    @Transactional
    public ChatMessage sendMessage(UUID sessionId, String content) {
        ChatSession session = chatSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ChatSessionNotFoundException(sessionId));

        String transcriptText = transcriptLookupPort.findTranscriptText(session.videoId())
                .orElseThrow(() -> new TranscriptNotReadyException(session.videoId()));

        List<ChatMessage> history = chatMessageRepository.findBySessionId(sessionId);
        chatMessageRepository.save(ChatMessage.create(sessionId, MessageRole.USER, content));

        String reply = chatCompletionPort.generateReply(transcriptText, history, content);
        return chatMessageRepository.save(ChatMessage.create(sessionId, MessageRole.ASSISTANT, reply));
    }

    @Transactional(readOnly = true)
    public List<ChatMessage> getMessages(UUID sessionId) {
        if (chatSessionRepository.findById(sessionId).isEmpty()) {
            throw new ChatSessionNotFoundException(sessionId);
        }
        return chatMessageRepository.findBySessionId(sessionId);
    }
}
