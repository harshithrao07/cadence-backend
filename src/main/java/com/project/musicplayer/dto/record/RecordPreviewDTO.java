package com.project.musicplayer.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.musicplayer.model.RecordType;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecordPreviewDTO(
        String id,
        String title,
        String releaseTimestamp,
        String coverUrl,
        RecordType recordType
) {
}
