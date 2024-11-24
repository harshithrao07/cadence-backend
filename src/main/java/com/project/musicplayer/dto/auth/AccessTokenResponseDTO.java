package com.project.musicplayer.dto.auth;

import jakarta.annotation.Nonnull;

public record AccessTokenResponseDTO(
        @Nonnull String accessToken,
        String message
) {
}
