package com.project.musicplayer.dto.artist;

import com.project.musicplayer.dto.genre.GenrePreviewDTO;
import com.project.musicplayer.dto.releases.ReleasesPreviewDTO;
import com.project.musicplayer.dto.song.TrackPreviewDTO;

import java.util.Map;
import java.util.Set;

public record ArtistProfileDTO(
        String id,
        String name,
        String profileUrl,
        String description,
        Set<TrackPreviewDTO> createdSongs,
        Set<ReleasesPreviewDTO> artistReleases,
        Set<ArtistProfileDTO> relatedArtists,
        Set<ReleasesPreviewDTO> artistAppearsOn,
        Map<GenrePreviewDTO, Long> genreUsed
) {
}
