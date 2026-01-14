package com.project.cadence.dto.song;

import com.project.cadence.dto.artist.ArtistPreviewDTO;

import java.util.ArrayList;
import java.util.List;

public record SongsInArtistProfileDTO(
        String songId,
        String title,
        Integer totalDuration,
        String coverUrl,
        Long totalPlays,
        String recordId,
        String recordTitle,
        List<ArtistPreviewDTO> artists
) {
    // JPQL-friendly constructor
    public SongsInArtistProfileDTO(
            String songId,
            String title,
            Integer totalDuration,
            String coverUrl,
            Long totalPlays,
            String recordId,
            String recordTitle
    ) {
        this(
                songId,
                title,
                totalDuration,
                coverUrl,
                totalPlays,
                recordId,
                recordTitle,
                new ArrayList<>()
        );
    }
}
