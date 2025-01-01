package com.project.musicplayer.dto.song;

import jakarta.validation.constraints.NotBlank;

import java.util.Set;

public record EachNewSongDTO(
        @NotBlank(message = "Song title cannot be empty") String title,
        @NotBlank(message = "Song URL cannot be empty") String songUrl,
        @NotBlank(message = "Genre ID's cannot be empty") Set<String> genreIds,
        @NotBlank(message = "Total Duration of cannot be empty") int totalDuration,
        Set<String> featureIds
) {
}
