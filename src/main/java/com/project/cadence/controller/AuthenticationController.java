package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.auth.*;
import com.project.cadence.service.AuthenticationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@Validated
@RequestMapping("/auth/v1")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<AuthenticationResponseDTO>> register(@Valid @RequestBody RegisterRequestDTO registerRequestDTO) {
        return authenticationService.register(registerRequestDTO);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponseDTO<AuthenticationResponseDTO>> authenticate(@Valid @RequestBody AuthenticateRequestDTO authenticateRequestDTO) {
        return authenticationService.authenticate(authenticateRequestDTO);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<String>> refreshToken(@Valid @RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        return authenticationService.refreshToken(refreshTokenRequestDTO);
    }
}
