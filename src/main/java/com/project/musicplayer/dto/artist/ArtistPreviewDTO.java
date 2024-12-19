package com.project.musicplayer.dto.artist;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ArtistPreviewDTO(
        String id,
        String name,
        String profileUrl
) {
}
