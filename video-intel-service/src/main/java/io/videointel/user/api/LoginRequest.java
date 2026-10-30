package io.videointel.user.api;

import jakarta.validation.constraints.NotBlank;

record LoginRequest(
        @NotBlank String email,
        @NotBlank String password
) {
}
