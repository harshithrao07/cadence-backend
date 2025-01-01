package com.project.musicplayer.dto.song;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Set;

public record NewSongsDTO(
        @NotBlank(message = "Record ID cannot be empty") String recordId,
        @NotNull Set<EachNewSongDTO> songs
        ) {
}
