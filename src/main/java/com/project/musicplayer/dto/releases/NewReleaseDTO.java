package com.project.musicplayer.dto.releases;

import com.project.musicplayer.model.ReleaseType;
import jakarta.annotation.Nonnull;

import java.util.Date;
import java.util.Set;

public record NewReleaseDTO(
        @Nonnull String title,
        long releaseTimestamp,
        String coverUrl,
        @Nonnull ReleaseType releaseType,
        @Nonnull Set<String> artistIds,
        Set<String> featureIds
) {
}
