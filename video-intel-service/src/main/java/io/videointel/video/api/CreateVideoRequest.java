package io.videointel.video.api;

import io.videointel.shared.ownership.OwnerType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

record CreateVideoRequest(
        @NotNull OwnerType ownerType,
        @NotNull UUID ownerId,
        @NotBlank String title
) {
}
