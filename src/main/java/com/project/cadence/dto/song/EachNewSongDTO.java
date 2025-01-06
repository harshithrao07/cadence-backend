package com.project.cadence.dto.song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record EachNewSongDTO(
        @NotBlank(message = "Song title cannot be empty") String title,
        @NotBlank(message = "Song URL cannot be empty") String songUrl,
        @NotNull Set<String> genreIds,
        @NotNull(message = "Total Duration cannot be empty") @Positive(message = "Total Duration must be a positive integer") int totalDuration,
        Set<String> featureIds
) {
}
