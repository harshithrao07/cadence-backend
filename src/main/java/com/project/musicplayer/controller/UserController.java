package com.project.musicplayer.controller;

import com.project.musicplayer.dto.user.UserIsFollowingDTO;
import com.project.musicplayer.dto.user.UserPreviewDTO;
import com.project.musicplayer.dto.user.UserProfileChangeDTO;
import com.project.musicplayer.dto.user.UserProfileDTO;
import com.project.musicplayer.service.JwtService;
import com.project.musicplayer.service.UserService;
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
    public ResponseEntity<UserProfileDTO> getUserProfile(HttpServletRequest request, @PathVariable("userId") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);

        // Whether the token email matches with the requested profile's email
        boolean matches = userService.authenticatedUserMatchesProfileLookup(tokenEmail, userId);
        return userService.getUserProfile(userId, matches);
    }

    @PutMapping(path = "/{userId}")
    public ResponseEntity<String> putUserProfile(HttpServletRequest request, @PathVariable("userId") String userId, @RequestBody UserProfileChangeDTO userProfileChangeDTO) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        // Whether the token email matches with the requested profile's email if not then unauthorized
        if (!userService.authenticatedUserMatchesProfileLookup(tokenEmail, userId)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Not authorized to edit profile");
        }
        return userService.putUserProfile(userId, userProfileChangeDTO);
    }

    @PostMapping(path = "/{userId}/follow")
    public ResponseEntity<String> followUser(HttpServletRequest request, @PathVariable("userId") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        // Prevents the user from following themselves
        if (userService.authenticatedUserMatchesProfileLookup(tokenEmail, userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You cannot follow yourself");
        }
        return userService.followUser(userId, tokenEmail);
    }

    @PostMapping(path = "/{userId}/unfollow")
    public ResponseEntity<String> unfollowUser(HttpServletRequest request, @PathVariable("userId") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        // Prevents the user from following themselves
        if (userService.authenticatedUserMatchesProfileLookup(tokenEmail, userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You cannot unfollow yourself");
        }
        return userService.unfollowUser(userId, tokenEmail);
    }

    @GetMapping(path = "/{userId}/is_following")
    public ResponseEntity<UserIsFollowingDTO> isFollowing(HttpServletRequest request, @PathVariable("userId") String userId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        if (userService.authenticatedUserMatchesProfileLookup(tokenEmail, userId)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        }
        return userService.isFollowing(userId, tokenEmail);
    }

    @GetMapping(path = "/{userId}/followers")
    public ResponseEntity<Set<UserPreviewDTO>> getUserFollowers(@PathVariable("userId") String userId) {
        return userService.getUserFollowers(userId);
    }

    @GetMapping(path = "/{userId}/following")
    public ResponseEntity<Set<UserPreviewDTO>> getUserFollowing(@PathVariable("userId") String userId) {
        return userService.getUserFollowing(userId);
    }

    @PostMapping(path = "/logout")
    public ResponseEntity<String> logOut(@NotNull HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            return userService.logOut(token);
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No token provided");
    }
}
