package com.project.musicplayer.dto.artist;

import jakarta.validation.constraints.NotBlank;

public record NewArtistDTO(
        @NotBlank(message = "Name cannot be empty") String name,
        String profileUrl,
        String description
) {
}
