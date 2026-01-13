package com.project.cadence.dto.artist;

import java.util.List;

public record PaginatedAllArtistsResponse(
        List<ArtistPreviewDTO> content,
        Integer page,
        Integer size,
        Long totalElements,
        Integer totalPages,
        Boolean last
) {
}
