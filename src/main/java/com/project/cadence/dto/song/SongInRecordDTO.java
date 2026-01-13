package com.project.cadence.dto.song;

import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.genre.GenrePreviewDTO;

import java.util.List;

public record SongInRecordDTO(
        String songId,
        String title,
        Integer totalDuration,
        String coverUrl,
        List<ArtistPreviewDTO> artists,
        List<GenrePreviewDTO> genres,
        Integer order
) {
}
