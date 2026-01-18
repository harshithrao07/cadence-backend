package com.project.cadence.dto.song;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Set;

public record NewSongsDTO(
        @NotBlank(message = "Record ID cannot be empty") String recordId,
        @NotNull @Valid List<EachNewSongDTO> songs
        ) {
}
