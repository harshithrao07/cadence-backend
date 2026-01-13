package com.project.cadence.dto.song;

public record SongsInArtistProfileDTO(
        String songId,
        String title,
        Integer totalDuration,
        String coverUrl,
        Long totalPlays,
        String recordId,
        String recordTitle
) {
}
