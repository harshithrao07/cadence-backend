package com.project.musicplayer.dto.artist;

import com.project.musicplayer.dto.record.RecordPreviewDTO;
import com.project.musicplayer.dto.song.TrackPreviewDTO;

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
