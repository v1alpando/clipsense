package io.videointel.user.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface AnonymousSessionRepository extends JpaRepository<AnonymousSessionEntity, UUID> {
}
