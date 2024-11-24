package com.project.musicplayer.dto.auth;

import jakarta.annotation.Nonnull;

public record RegisterRequestDTO(
        @Nonnull String name,
        @Nonnull String email,
        @Nonnull String password
) {
}
