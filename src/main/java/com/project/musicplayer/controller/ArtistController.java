package com.project.musicplayer.controller;

import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.artist.ArtistProfileDTO;
import com.project.musicplayer.dto.artist.NewArtistDTO;
import com.project.musicplayer.dto.user.UserPreviewDTO;
import com.project.musicplayer.service.ArtistService;
import com.project.musicplayer.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/artist")
public class ArtistController {
    private final JwtService jwtService;
    private final ArtistService artistService;

    @GetMapping(path = "/{artistId}")
    public ResponseEntity<ApiResponseDTO<ArtistProfileDTO>> getArtistProfile(@PathVariable("artistId") String artistId) {
        return artistService.getArtistProfile(artistId);
    }

    @PostMapping(path = "/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewArtist(HttpServletRequest request, @RequestBody NewArtistDTO newArtistDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return artistService.addNewArtist(newArtistDTO);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @PostMapping(path = "/{artistId}/follow")
    public ResponseEntity<ApiResponseDTO<Void>> followArtist(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.followArtist(artistId, tokenEmail);
    }

    @PostMapping(path = "/{artistId}/unfollow")
    public ResponseEntity<ApiResponseDTO<Void>> unfollowArtist(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.unfollowArtist(artistId, tokenEmail);
    }

    @GetMapping(path = "/{artistId}/is_following")
    public ResponseEntity<ApiResponseDTO<Boolean>> isFollowing(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.isFollowing(artistId, tokenEmail);
    }

    @GetMapping(path = "/{artistId}/followers")
    public ResponseEntity<ApiResponseDTO<Set<UserPreviewDTO>>> getArtistFollowers(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.getArtistFollowers(artistId, tokenEmail);
    }
}
