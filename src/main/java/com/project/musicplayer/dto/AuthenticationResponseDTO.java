package com.project.musicplayer.dto;

import jakarta.annotation.Nonnull;

public record AuthenticationResponseDTO(
        @Nonnull String accessToken,
        @Nonnull String refreshToken,
        String message
) {
}
