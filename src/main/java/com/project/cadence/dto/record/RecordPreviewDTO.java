package com.project.cadence.dto.record;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.model.RecordType;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RecordPreviewDTO(
        String id,
        String title,
        long releaseTimestamp,
        String coverUrl,
        RecordType recordType,
        List<ArtistPreviewDTO> artists
) {
}
