package com.project.musicplayer.service;

import com.project.musicplayer.model.InvalidatedToken;
import com.project.musicplayer.model.User;
import com.project.musicplayer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public Optional<User> loadUserByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    public ResponseEntity<String> logOut(@NotNull String token) {
        try {
            String[] sections = token.split("\\.");

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(sections[1]));

            String userEmail = jwtService.extractEmailForPayload(payload);
            InvalidatedToken invalidatedToken = jwtService.invalidateAllUserTokens(userEmail);
            if (invalidatedToken == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while logging out");
            }
            return ResponseEntity.status(HttpStatus.OK).body("Logged out successfully");
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred in the server");
        }
    }

}
