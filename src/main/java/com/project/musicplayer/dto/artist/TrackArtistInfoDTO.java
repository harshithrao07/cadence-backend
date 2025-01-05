package com.project.musicplayer.dto.artist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TrackArtistInfoDTO(
        String id,
        String name
) {
}
