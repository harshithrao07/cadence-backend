package com.project.cadence.dto.song;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.cadence.dto.record.TrackRecordInfoDTO;
import com.project.cadence.dto.artist.TrackArtistInfoDTO;

import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrackPreviewDTO(
        String id,
        String title,
        Integer totalDuration,
        String coverUrl,
        Long totalPlays,
        List<TrackArtistInfoDTO> trackArtistInfo,
        TrackRecordInfoDTO trackRecordInfo
) {
}
