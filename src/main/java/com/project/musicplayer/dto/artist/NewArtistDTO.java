package com.project.musicplayer.dto.artist;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record NewArtistDTO(
        @NotBlank(message = "Cannot be empty") @NotNull(message = "Cannot be null") String name,
        String profileUrl,
        String description
) {
}
