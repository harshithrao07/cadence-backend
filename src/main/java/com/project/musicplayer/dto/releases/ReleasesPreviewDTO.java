package com.project.musicplayer.dto.releases;

import com.project.musicplayer.model.ReleaseType;

public record ReleasesPreviewDTO(
        String id,
        String title,
        String releaseYear,
        String coverUrl,
        ReleaseType releaseType
) {
}
