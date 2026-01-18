package com.project.cadence.dto.record;

import com.project.cadence.model.RecordType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record NewRecordDTO(
        @NotBlank(message = "Record title cannot be empty") String title,
        @NotNull(message = "Release Timestamp cannot be null") Long releaseTimestamp,
        @NotNull(message = "Record Type cannot be null") RecordType recordType,
        @NotNull(message = "Artist Id's cannot be empty") List<String> artistIds
) {
}
