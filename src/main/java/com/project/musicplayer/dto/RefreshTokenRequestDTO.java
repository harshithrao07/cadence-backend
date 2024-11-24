package com.project.musicplayer.dto;

import jakarta.annotation.Nonnull;

public record RefreshTokenRequestDTO(
        @Nonnull String refreshToken
) {
}
