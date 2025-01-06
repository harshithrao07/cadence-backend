package com.project.cadence.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequestDTO(
        @NotBlank(message = "Refresh token cannot be blank") String refreshToken
) {
}
