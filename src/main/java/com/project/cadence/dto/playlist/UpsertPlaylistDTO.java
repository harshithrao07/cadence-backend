package com.project.cadence.dto.playlist;

import com.project.cadence.model.PlaylistVisibility;
import jakarta.validation.constraints.NotBlank;

import java.util.Optional;

public record UpsertPlaylistDTO(
        Optional<String> id,
        @NotBlank(message = "Playlist name cannot be empty") String name,
        Optional<PlaylistVisibility> visibility
) {
}
