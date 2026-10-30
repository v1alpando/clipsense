package io.videointel.chat.domain;

import io.videointel.shared.exception.NotFoundException;

import java.util.UUID;

public class ChatSessionNotFoundException extends NotFoundException {

    public ChatSessionNotFoundException(UUID id) {
        super("Chat session not found: " + id);
    }
}
