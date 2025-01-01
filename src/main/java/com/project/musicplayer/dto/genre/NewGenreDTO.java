package com.project.musicplayer.dto.genre;

import jakarta.validation.constraints.NotBlank;

public record NewGenreDTO(
        @NotBlank(message = "Genre type cannot be blank") String type
) {
}
