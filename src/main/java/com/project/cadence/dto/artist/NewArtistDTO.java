package com.project.cadence.dto.artist;

import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

public record NewArtistDTO(
        @NotBlank(message = "Name cannot be empty") String name,
        Optional<String> profileUrl,
        Optional<String> description
) {
}
