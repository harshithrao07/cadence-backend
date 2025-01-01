package com.project.musicplayer.dto.record;

import com.project.musicplayer.model.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Optional;
import java.util.Set;

public record NewRecordDTO(
        @NotBlank(message = "Record title cannot be empty") String title,
        Optional<Long> releaseTimestamp,
        Optional<String> coverUrl,
        @NotNull(message = "Record Type cannot be null") RecordType recordType,
        @NotNull(message = "Artist Id's cannot be empty") Set<String> artistIds
) {
}
