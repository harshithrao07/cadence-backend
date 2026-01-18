package com.project.cadence.dto.song;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public record UpdateSongDTO(
        String id,
        Optional<String> title,
        Optional<Integer> totalDuration,
        Optional<Set<String>> genreIds,
        Optional<List<String>> artistIds,
        int order
) {
}
