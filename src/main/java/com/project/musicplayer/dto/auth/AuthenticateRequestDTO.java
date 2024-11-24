package com.project.musicplayer.dto.auth;

import jakarta.annotation.Nonnull;

public record AuthenticateRequestDTO(
        @Nonnull String email,
        @Nonnull String password
) {
}
