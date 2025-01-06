package com.project.cadence.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record AuthenticateRequestDTO(
        @NotBlank(message = "Email cannot be blank") String email,
        @NotBlank(message = "Password cannot be blank") String password
) {
}
