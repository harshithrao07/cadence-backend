package com.project.musicplayer.controller;

import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.auth.*;
import com.project.musicplayer.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequiredArgsConstructor
@RestController
@RequestMapping("/auth/v1")
public class AuthenticationController {
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<AuthenticationResponseDTO>> register(@RequestBody RegisterRequestDTO registerRequestDTO) {
        return authenticationService.register(registerRequestDTO);
    }

    @PostMapping("/authenticate")
    public ResponseEntity<ApiResponseDTO<AuthenticationResponseDTO>> authenticate(@RequestBody AuthenticateRequestDTO authenticateRequestDTO) {
        return authenticationService.authenticate(authenticateRequestDTO);
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponseDTO<String>> refreshToken(@RequestBody RefreshTokenRequestDTO refreshTokenRequestDTO) {
        return authenticationService.refreshToken(refreshTokenRequestDTO);
    }
}
