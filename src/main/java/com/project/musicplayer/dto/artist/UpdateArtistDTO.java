package com.project.musicplayer.dto.artist;

import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

public record UpdateArtistDTO(
        String name,
        String profileUrl,
        String description
) {
}
