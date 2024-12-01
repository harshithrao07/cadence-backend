package com.project.musicplayer.dto.artist;

import jakarta.annotation.Nonnull;

public record NewArtistDTO(
        @Nonnull String name,
        String profileUrl,
        String description
) {
}
