package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.PaginatedResponseDTO;
import com.project.cadence.dto.artist.*;
import com.project.cadence.dto.user.UserPreviewDTO;
import com.project.cadence.service.ArtistService;
import com.project.cadence.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/artist")
public class ArtistController {
    private final JwtService jwtService;
    private final ArtistService artistService;

    @PostMapping(path = "/upsert")
    public ResponseEntity<ApiResponseDTO<String>> upsertArtist(HttpServletRequest request, @Validated @RequestBody UpsertArtistDTO upsertArtistDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return artistService.upsertArtist(upsertArtistDTO);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @DeleteMapping(path = "/delete/{artistId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingArtist(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return artistService.deleteExistingArtist(artistId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping(path = "/all")
    public ResponseEntity<ApiResponseDTO<PaginatedResponseDTO<ArtistPreviewDTO>>> getAllArtists(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "24") int size, @RequestParam(required = false) String key) {
        return artistService.getAllArtists(page, size, key);
    }

    @GetMapping(path = "/{artistId}")
    public ResponseEntity<ApiResponseDTO<ArtistProfileDTO>> getArtistProfile(@PathVariable("artistId") String artistId) {
        return artistService.getArtistProfile(artistId);
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

    @GetMapping(path = "/{artistId}/isFollowing")
    public ResponseEntity<ApiResponseDTO<Boolean>> isFollowing(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.isFollowing(artistId, tokenEmail);
    }

    @GetMapping(path = "/{artistId}/followers")
    public ResponseEntity<ApiResponseDTO<List<UserPreviewDTO>>> getArtistFollowers(@PathVariable("artistId") String artistId) {
        return artistService.getArtistFollowers(artistId);
    }

}
