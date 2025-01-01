package com.project.musicplayer.dto.record;

import jakarta.validation.constraints.NotBlank;

import java.util.Optional;
import java.util.Set;

public record UpdateRecordDTO(
        Optional<String> title,
        Optional<Long> releaseTimestamp,
        Optional<String> coverUrl,
        Optional<Set<String>> artistIds
) {
}
