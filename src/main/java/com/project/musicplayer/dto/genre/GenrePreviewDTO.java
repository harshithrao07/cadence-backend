package com.project.musicplayer.dto.genre;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GenrePreviewDTO(
        String id,
        String type
) {
}
