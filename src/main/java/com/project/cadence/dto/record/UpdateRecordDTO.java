package com.project.cadence.dto.record;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record UpdateRecordDTO(
        Optional<String> title,
        Optional<Long> releaseTimestamp,
        Optional<List<String>> artistIds
) {
}
