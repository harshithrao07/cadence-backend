package com.project.cadence.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.cadence.model.RecordType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecordPreviewDTO(
        String id,
        String title,
        long releaseTimestamp,
        String coverUrl,
        RecordType recordType
) {
}
