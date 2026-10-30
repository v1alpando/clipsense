package io.videointel.user.api;

import io.videointel.user.application.AnonymousSessionService;
import io.videointel.user.domain.AnonymousSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/anonymous-sessions")
@RequiredArgsConstructor
class AnonymousSessionController {

    private final AnonymousSessionService anonymousSessionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    AnonymousSessionResponse create() {
        return toResponse(anonymousSessionService.create());
    }

    @GetMapping("/{id}")
    AnonymousSessionResponse get(@PathVariable UUID id) {
        return toResponse(anonymousSessionService.get(id));
    }

    private AnonymousSessionResponse toResponse(AnonymousSession session) {
        return new AnonymousSessionResponse(session.id(), session.createdAt(), session.expiresAt());
    }
}
