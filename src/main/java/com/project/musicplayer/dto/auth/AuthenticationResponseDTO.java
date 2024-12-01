package com.project.musicplayer.dto.auth;

import jakarta.annotation.Nonnull;

public record AuthenticationResponseDTO(
        @Nonnull String id,
        @Nonnull String accessToken,
        @Nonnull String refreshToken
) {
}
