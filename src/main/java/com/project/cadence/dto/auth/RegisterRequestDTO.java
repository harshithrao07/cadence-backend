package com.project.cadence.dto.auth;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;

import java.util.Date;

public record RegisterRequestDTO(
        @NotBlank(message = "Name cannot be blank") String name,
        @NotBlank(message = "Email cannot be blank") String email,
        @NotBlank(message = "Password cannot be blank") String password,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSX")
        Date dateOfBirth
) {
}
