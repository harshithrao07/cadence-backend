package com.project.cadence.service;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.auth.*;
import com.project.cadence.model.*;
import com.project.cadence.repository.PlaylistRepository;
import com.project.cadence.repository.UserRepository;
import com.project.cadence.auth.TokenType;
import jakarta.validation.Valid;
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
    private final PlaylistRepository playlistRepository;
    private final UserService userService;

    public ResponseEntity<ApiResponseDTO<AuthenticationResponseDTO>> register(@NotNull RegisterRequestDTO registerRequestDTO) {
        try {
            if (userRepository.existsByEmail(registerRequestDTO.email())) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new ApiResponseDTO<>(false, "A user with the given email already exists", null));
            }

            String password = registerRequestDTO.password();
            if (password.isBlank() ||
                    password.length() < 10 ||
                    !password.matches(".*([0-9]|[!@#$%^&*(),.?\":{}|<>]).*")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponseDTO<>(false, "Password must be at least 10 characters long and contain at least one special character", null));
            }

            User user = User.builder()
                    .name(registerRequestDTO.name())
                    .email(registerRequestDTO.email())
                    .passwordHash(passwordEncoder.encode(password))
                    .build();

            User savedUser = userRepository.save(user);
            Playlist likedSongs = Playlist.builder()
                    .name("Liked Songs")
                    .owner(savedUser)
                    .isSystem(true)
                    .visibility(PlaylistVisibility.PRIVATE)
                    .systemType(SystemPlaylistType.LIKED_SONGS)
                    .build();
            playlistRepository.save(likedSongs);

            if (savedUser.getId() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred while creating the user", null));
            }

            String accessToken = jwtService.generateJwtToken(savedUser.getEmail(), savedUser.getRole(), TokenType.access);
            String refreshToken = jwtService.generateJwtToken(savedUser.getEmail(), savedUser.getRole(), TokenType.refresh);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true, "User registered successfully", new AuthenticationResponseDTO(savedUser.getId(), accessToken, refreshToken)));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<AuthenticationResponseDTO>> authenticate(AuthenticateRequestDTO authenticateRequestDTO) {
        try {
            Optional<User> user = userRepository.findByEmail(authenticateRequestDTO.email());
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "User does not exist", null));
            }

            if (!passwordEncoder.matches(authenticateRequestDTO.password(), user.get().getPasswordHash())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Password does not match", null));
            }

            String accessToken = jwtService.generateJwtToken(user.get().getEmail(), user.get().getRole(), TokenType.access);
            String refreshToken = jwtService.generateJwtToken(user.get().getEmail(), user.get().getRole(), TokenType.refresh);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "User authenticated successfully", new AuthenticationResponseDTO(user.get().getId(), accessToken, refreshToken)));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<String>> refreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO) {
        try {
            String[] sections = refreshTokenRequestDTO.refreshToken().split("\\.");

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String header = new String(decoder.decode(sections[0]));
            String payload = new String(decoder.decode(sections[1]));
            String tokenSignature = sections[2];

            String expectedSignature = jwtService.generateSignature(header, payload);
            if (!tokenSignature.equals(expectedSignature)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "Token integrity verification failed", null));
            }

            if (jwtService.isTokenExpired(payload)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponseDTO<>(false, "Refresh token has expired", null));
            }

            String userEmail = jwtService.extractEmailForPayload(payload);
            if (userEmail == null || userEmail.isEmpty() || !userRepository.existsByEmail(userEmail)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "Unable to extract user email from token", null));
            }

            Role role = userService.getUserRoleFromRepository(userEmail);
            if (role == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponseDTO<>(false, "User no longer exists", null));
            }

            String accessToken = jwtService.generateJwtToken(userEmail, role, TokenType.access);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "New Access Token successfully generated", accessToken));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Boolean>> validateEmail(@Valid String email) {
        try {
            boolean exists = userRepository.existsByEmail(email);
            String message = exists ? "User already exists" : "User does not exist";
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, message, exists));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }
}
