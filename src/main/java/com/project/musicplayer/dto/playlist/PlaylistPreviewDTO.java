package com.project.musicplayer.dto.playlist;

import com.project.musicplayer.model.PlaylistVisibility;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

public record PlaylistPreviewDTO(
        String id,
        String name,
        String coverUrl,
        PlaylistVisibility visibility
) {
}
