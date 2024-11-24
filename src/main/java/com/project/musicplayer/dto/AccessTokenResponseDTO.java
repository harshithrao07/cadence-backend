package com.project.musicplayer.dto;

import jakarta.annotation.Nonnull;

public record AccessTokenResponseDTO(
        @Nonnull String accessToken,
        String message
) {
}
