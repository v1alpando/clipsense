package io.videointel.chat.api;

import io.videointel.shared.ownership.OwnerType;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record CreateChatSessionRequest(
        @NotNull OwnerType ownerType,
        @NotNull UUID ownerId
) {
}
