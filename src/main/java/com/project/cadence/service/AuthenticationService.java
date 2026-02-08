package com.project.cadence.service;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.auth.*;
import com.project.cadence.events.UserCreatedEvent;
import com.project.cadence.model.*;
import com.project.cadence.repository.UserRepository;
import com.project.cadence.utils.JwtUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthenticationService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher publisher;
    private final JwtUtil jwtUtil;

    @Transactional
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
            publisher.publishEvent(new UserCreatedEvent(savedUser.getId()));

            if (savedUser.getId() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred while creating the user", null));
            }

            String accessToken = jwtUtil.generateToken(savedUser.getEmail(), 15);
            String refreshToken = jwtUtil.generateToken(savedUser.getEmail(), 7 * 24 * 60);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/auth/v1/refresh")
                    .maxAge(7 * 24 * 60 * 60)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new ApiResponseDTO<>(
                            true,
                            "User registered successfully",
                            new AuthenticationResponseDTO(savedUser.getId(), accessToken)
                    ));
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

            String accessToken = jwtUtil.generateToken(user.get().getEmail(), 15);
            String refreshToken = jwtUtil.generateToken(user.get().getEmail(), 7 * 24 * 60);

            ResponseCookie cookie = ResponseCookie.from("refreshToken", refreshToken)
                    .httpOnly(true)
                    .secure(true)
                    .sameSite("None")
                    .path("/auth/v1/refresh")
                    .maxAge(7 * 24 * 60 * 60)
                    .build();

            return ResponseEntity.status(HttpStatus.CREATED)
                    .header(HttpHeaders.SET_COOKIE, cookie.toString())
                    .body(new ApiResponseDTO<>(
                            true,
                            "User registered successfully",
                            new AuthenticationResponseDTO(user.get().getId(), accessToken)
                    ));
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

    public ResponseEntity<String> logout(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
        return ResponseEntity.ok("Logged out successfully");
    }
}
