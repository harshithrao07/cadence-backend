package com.project.cadence.dto.artist;

import java.util.Optional;

public record UpdateArtistDTO(
        Optional<String> name,
        Optional<String> description
) {
}
