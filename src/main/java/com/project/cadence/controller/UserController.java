package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.user.UserProfileChangeDTO;
import com.project.cadence.dto.user.UserProfileDTO;
import com.project.cadence.model.Role;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/user")
public class UserController {
    private final UserService userService;
    private final JwtService jwtService;

    @GetMapping(path = "/{userId}")
    public ResponseEntity<ApiResponseDTO<UserProfileDTO>> getUserProfile(HttpServletRequest request, @PathVariable("userId") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return userService.getUserProfile(userId, tokenEmail);
    }

    @PutMapping(path = "/{userId}")
    public ResponseEntity<ApiResponseDTO<Void>> putUserProfile(HttpServletRequest request, @PathVariable("userId") String userId, @RequestBody UserProfileChangeDTO userProfileChangeDTO) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return userService.putUserProfile(userId, userProfileChangeDTO, tokenEmail);
    }

    @GetMapping(path = "/isAdmin")
    public ResponseEntity<Boolean> isAdmin(@NotNull HttpServletRequest request) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return ResponseEntity.status(HttpStatus.OK).body(true);
        }
        return ResponseEntity.status(HttpStatus.OK).body(false);
    }
}
