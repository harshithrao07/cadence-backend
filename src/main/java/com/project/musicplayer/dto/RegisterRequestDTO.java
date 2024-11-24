package com.project.musicplayer.dto;

import jakarta.annotation.Nonnull;

public record RegisterRequestDTO(
        @Nonnull String firstName,
        String lastName,
        @Nonnull String email,
        @Nonnull String password
) {
}
