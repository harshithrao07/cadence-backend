package com.project.musicplayer.dto.artist;

import jakarta.annotation.Nonnull;

public record UpdateArtistDTO(
        String name,
        String profileUrl,
        String description
) {
}
