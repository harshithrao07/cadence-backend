package com.project.musicplayer.dto.auth;

import jakarta.annotation.Nonnull;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(
        @NotBlank(message = "Refresh token cannot be blank") String refreshToken
) {
}
