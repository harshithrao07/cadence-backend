package com.project.cadence.dto.playlist;

import com.project.cadence.model.PlaylistVisibility;

public record PlaylistPreviewDTO(
        String id,
        String name,
        String coverUrl,
        PlaylistVisibility visibility
) {
}
