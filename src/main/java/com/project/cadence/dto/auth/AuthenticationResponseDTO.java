package com.project.cadence.dto.auth;

import com.project.cadence.model.Role;
import jakarta.annotation.Nonnull;

public record AuthenticationResponseDTO(
        @Nonnull String id,
        @Nonnull String accessToken,
        @Nonnull String refreshToken
) {
}
