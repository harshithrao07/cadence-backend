package com.project.musicplayer.dto;

import jakarta.annotation.Nonnull;

public record AuthenticateRequestDTO(
        @Nonnull String email,
        @Nonnull String password
) {
}
