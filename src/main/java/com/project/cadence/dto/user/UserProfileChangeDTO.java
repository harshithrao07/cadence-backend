package com.project.cadence.dto.user;

import java.util.Optional;

public record UserProfileChangeDTO(
        Optional<String> name
) {
}
