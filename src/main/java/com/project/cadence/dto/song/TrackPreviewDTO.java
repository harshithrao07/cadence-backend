package com.project.cadence.dto.song;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.cadence.dto.record.TrackRecordInfoDTO;
import com.project.cadence.dto.artist.TrackArtistInfoDTO;

import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrackPreviewDTO(
        String id,
        String title,
        int totalDuration,
        String coverUrl,
        Set<TrackArtistInfoDTO> trackArtistInfo,
        TrackRecordInfoDTO trackRecordInfoDTO
) {
}
