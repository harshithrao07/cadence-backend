package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.user.UserPreviewDTO;
import com.project.cadence.dto.user.UserProfileChangeDTO;
import com.project.cadence.dto.user.UserProfileDTO;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

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

    @PostMapping(path = "/{userId}/follow")
    public ResponseEntity<ApiResponseDTO<Void>> followUser(HttpServletRequest request, @PathVariable("userId") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return userService.followUser(userId, tokenEmail);
    }

    @PostMapping(path = "/{userId}/unfollow")
    public ResponseEntity<ApiResponseDTO<Void>> unfollowUser(HttpServletRequest request, @PathVariable("userId") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return userService.unfollowUser(userId, tokenEmail);
    }

    @GetMapping(path = "/{userId}/is_following")
    public ResponseEntity<ApiResponseDTO<Boolean>> isFollowing(HttpServletRequest request, @PathVariable("userId") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return userService.isFollowing(userId, tokenEmail);
    }

    @GetMapping(path = "/{userId}/followers")
    public ResponseEntity<ApiResponseDTO<Set<UserPreviewDTO>>> getUserFollowers(@PathVariable("userId") String userId) {
        return userService.getUserFollowers(userId);
    }

    @GetMapping(path = "/{userId}/following")
    public ResponseEntity<ApiResponseDTO<Set<UserPreviewDTO>>> getUserFollowing(@PathVariable("userId") String userId) {
        return userService.getUserFollowing(userId);
    }

    @PostMapping(path = "/logout")
    public ResponseEntity<ApiResponseDTO<Void>> logOut(@NotNull HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return userService.logOut(token);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "No token provided", null));
    }
}
