package com.project.cadence.dto.artist;

import com.project.cadence.dto.record.RecordPreviewDTO;
import com.project.cadence.dto.song.TrackPreviewDTO;

import java.util.Set;

public record ArtistProfileDTO(
        String id,
        String name,
        String profileUrl,
        String description,
        Set<TrackPreviewDTO> createdSongs,
        Set<RecordPreviewDTO> artistRecords,
        Set<RecordPreviewDTO> artistAppearsOn
) {
}
