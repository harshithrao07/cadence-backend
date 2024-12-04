package com.project.musicplayer.dto.song;

public record NewSongDTO(
        String title,
        String songUrl,
        String coverUrl
) {
}
