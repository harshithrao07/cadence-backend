package com.project.musicplayer.service;

import com.project.musicplayer.dto.auth.*;
import com.project.musicplayer.model.Role;
import com.project.musicplayer.model.User;
import com.project.musicplayer.repository.UserRepository;
import com.project.musicplayer.utility.TokenType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public ResponseEntity<AuthenticationResponseDTO> register(@NotNull RegisterRequestDTO registerRequestDTO) {
        try {
            if (userRepository.existsByEmail(registerRequestDTO.email())) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new AuthenticationResponseDTO("", "", "", "A user with the given email already exists"));
            }

            User user = User.builder()
                    .name(registerRequestDTO.name())
                    .email(registerRequestDTO.email())
                    .password(passwordEncoder.encode(registerRequestDTO.password()))
                    .role(Role.USER)
                    .build();

            User savedUser = userRepository.save(user);
            if (savedUser.getId() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthenticationResponseDTO("", "", "", "An error occurred while creating the user"));
            }

            String accessToken = jwtService.generateJwtToken(savedUser.getEmail(), savedUser.getRole(), TokenType.access);
            String refreshToken = jwtService.generateJwtToken(savedUser.getEmail(), savedUser.getRole(), TokenType.refresh);
            return ResponseEntity.status(HttpStatus.OK).body(new AuthenticationResponseDTO(savedUser.getId().toString(), accessToken, refreshToken, "User registered successfully"));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthenticationResponseDTO("", "", "", "An error occurred in the server"));
        }
    }

    public ResponseEntity<AuthenticationResponseDTO> authenticate(AuthenticateRequestDTO authenticateRequestDTO) {
        try {
            Optional<User> user = userRepository.findByEmail(authenticateRequestDTO.email());
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthenticationResponseDTO("", "", "", "User does not exist"));
            }

            if (!passwordEncoder.matches(authenticateRequestDTO.password(), user.get().getPassword())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AuthenticationResponseDTO("", "", "", "Password does not match"));
            }

            String accessToken = jwtService.generateJwtToken(user.get().getEmail(), user.get().getRole(), TokenType.access);
            String refreshToken = jwtService.generateJwtToken(user.get().getEmail(), user.get().getRole(), TokenType.refresh);
            return ResponseEntity.status(HttpStatus.OK).body(new AuthenticationResponseDTO(user.get().getId().toString(), accessToken, refreshToken, "User authenticated successfully"));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AuthenticationResponseDTO("", "", "", "An error occurred in the server"));
        }
    }

    public ResponseEntity<AccessTokenResponseDTO> refreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        try {
            String[] sections = refreshTokenRequestDTO.refreshToken().split("\\.");

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String header = new String(decoder.decode(sections[0]));
            String payload = new String(decoder.decode(sections[1]));
            String tokenSignature = sections[2];

            String expectedSignature = jwtService.generateSignature(header, payload);
            if (!tokenSignature.equals(expectedSignature)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AccessTokenResponseDTO("", "Token integrity verification failed"));
            }

            String userEmail = jwtService.extractEmailForPayload(payload);
            if (userEmail == null || userEmail.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AccessTokenResponseDTO("", "Unable to extract user email from token"));
            }

            Role role;
            try {
                role = Role.valueOf(jwtService.extractRoleForPayload(payload));
            } catch (IllegalArgumentException e) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new AccessTokenResponseDTO("", "Unable to extract the user role from token"));
            }

            if (jwtService.isTokenExpired(payload)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new AccessTokenResponseDTO("", "Refresh token has expired"));
            }

            String accessToken = jwtService.generateJwtToken(userEmail, role, TokenType.access);
            return ResponseEntity.status(HttpStatus.OK).body(new AccessTokenResponseDTO(accessToken, "New Access Token successfully generated"));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new AccessTokenResponseDTO("", "An error occurred in the server"));
        }
    }
}
