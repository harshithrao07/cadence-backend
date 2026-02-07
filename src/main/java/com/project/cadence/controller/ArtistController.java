package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.PaginatedResponseDTO;
import com.project.cadence.dto.artist.*;
import com.project.cadence.dto.user.UserPreviewDTO;
import com.project.cadence.service.ArtistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/artist")
public class ArtistController {
    private final ArtistService artistService;

    @PostMapping(path = "/upsert")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<String>> upsertArtist(@Validated @RequestBody UpsertArtistDTO upsertArtistDTO) {
        return artistService.upsertArtist(upsertArtistDTO);
    }

    @DeleteMapping(path = "/delete/{artistId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingArtist(@PathVariable("artistId") String artistId) {
        return artistService.deleteExistingArtist(artistId);
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
    public ResponseEntity<ApiResponseDTO<Void>> followArtist(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("artistId") String artistId) {
        return artistService.followArtist(artistId, userDetails.getUsername());
    }

    @PostMapping(path = "/{artistId}/unfollow")
    public ResponseEntity<ApiResponseDTO<Void>> unfollowArtist(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("artistId") String artistId) {
        return artistService.unfollowArtist(artistId, userDetails.getUsername());
    }

    @GetMapping(path = "/{artistId}/isFollowing")
    public ResponseEntity<ApiResponseDTO<Boolean>> isFollowing(@AuthenticationPrincipal UserDetails userDetails, @PathVariable("artistId") String artistId) {
        return artistService.isFollowing(artistId, userDetails.getUsername());
    }

    @GetMapping(path = "/{artistId}/followers")
    public ResponseEntity<ApiResponseDTO<List<UserPreviewDTO>>> getArtistFollowers(@PathVariable("artistId") String artistId) {
        return artistService.getArtistFollowers(artistId);
    }

}
