package io.videointel.chat.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface ChatSessionRepository extends JpaRepository<ChatSessionEntity, UUID> {
}
