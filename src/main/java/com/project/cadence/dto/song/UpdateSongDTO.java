package com.project.cadence.dto.song;

import java.util.Optional;
import java.util.Set;

public record UpdateSongDTO(
        Optional<String> title,
        Optional<Integer> totalDuration,
        Optional<Set<String>> genreIds,
        Optional<Set<String>> artistIds,
        Optional<String> coverUrl
) {
}
