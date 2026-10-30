package io.videointel.chat.api;

import jakarta.validation.constraints.NotBlank;

record SendMessageRequest(
        @NotBlank String content
) {
}
