package com.project.musicplayer.dto.song;

import java.util.Optional;
import java.util.Set;

public record UpdateSongDTO(
        Optional<String> title,
        Optional<String> songUrl,
        Optional<Integer> totalDuration,
        Optional<String> coverUrl,
        Optional<Set<String>> genreIds,
        Optional<Set<String>> featureIds
) {
}
