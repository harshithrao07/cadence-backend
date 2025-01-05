package com.project.musicplayer.dto.song;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.musicplayer.dto.record.TrackRecordInfoDTO;
import com.project.musicplayer.dto.artist.TrackArtistInfoDTO;

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
