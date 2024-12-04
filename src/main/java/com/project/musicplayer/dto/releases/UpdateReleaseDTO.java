package com.project.musicplayer.dto.releases;

import com.project.musicplayer.model.ReleaseType;
import jakarta.annotation.Nonnull;

import java.util.Set;

public record UpdateReleaseDTO(
        String title,
        long releaseTimestamp,
        String coverUrl,
        Set<String> artistIds,
        Set<String> featureIds
) {
}
