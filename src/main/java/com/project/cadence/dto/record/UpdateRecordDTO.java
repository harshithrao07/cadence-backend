package com.project.cadence.dto.record;

import java.util.Optional;
import java.util.Set;

public record UpdateRecordDTO(
        Optional<String> title,
        Optional<Long> releaseTimestamp,
        Optional<Set<String>> artistIds
) {
}
