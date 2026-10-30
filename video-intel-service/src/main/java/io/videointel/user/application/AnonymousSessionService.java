package io.videointel.user.application;

import io.videointel.user.domain.AnonymousSession;
import io.videointel.user.domain.AnonymousSessionNotFoundException;
import io.videointel.user.domain.AnonymousSessionRepositoryPort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnonymousSessionService {

    private final AnonymousSessionRepositoryPort anonymousSessionRepository;

    @Transactional
    public AnonymousSession create() {
        AnonymousSession session = AnonymousSession.create();
        return anonymousSessionRepository.save(session);
    }

    @Transactional(readOnly = true)
    public AnonymousSession get(UUID id) {
        return anonymousSessionRepository.findById(id)
                .orElseThrow(() -> new AnonymousSessionNotFoundException(id));
    }
}
