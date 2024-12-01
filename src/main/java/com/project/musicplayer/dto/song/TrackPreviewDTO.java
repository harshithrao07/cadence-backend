package com.project.musicplayer.dto.song;

import com.project.musicplayer.dto.releases.TrackReleaseInfoDTO;
import com.project.musicplayer.dto.artist.TrackArtistInfoDTO;

import java.util.Set;

public record TrackPreviewDTO(
        String id,
        String title,
        int totalDuration,
        String coverUrl,
        Set<TrackArtistInfoDTO> trackArtistInfo,
        TrackReleaseInfoDTO trackReleaseInfoDTO
) {
}
