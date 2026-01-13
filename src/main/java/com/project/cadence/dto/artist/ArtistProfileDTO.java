package com.project.cadence.dto.artist;

import com.project.cadence.dto.record.RecordPreviewDTO;
import com.project.cadence.dto.song.SongsInArtistProfileDTO;
import com.project.cadence.dto.song.TrackPreviewDTO;

import java.util.List;
import java.util.Set;

public record ArtistProfileDTO(
        String id,
        String name,
        String profileUrl,
        String description,
        Long followers,
        Long monthlyListeners,
        List<SongsInArtistProfileDTO> popularSongs,
        Set<RecordPreviewDTO> artistRecords
) {
}
