package com.project.musicplayer.controller;

import com.project.musicplayer.dto.user.CurrentUserProfileDTO;
import com.project.musicplayer.service.JwtService;
import com.project.musicplayer.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping(path = "{userEmail}")
    public ResponseEntity<CurrentUserProfileDTO> getUserProfile(HttpServletRequest request, @PathVariable("userEmail") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        if (tokenEmail.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        // Ensure that the token email matches the requested profile's email
        boolean isAuthorized = userService.isAuthorizedToAccessProfile(tokenEmail, userId);
        if (!isAuthorized) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        return userService.getUserProfile(tokenEmail, userId);
    }

    @GetMapping(path = "/logout")
    public ResponseEntity<String> logOut(@NotNull HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return userService.logOut(token);
        }

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No token provided");
    }

}
