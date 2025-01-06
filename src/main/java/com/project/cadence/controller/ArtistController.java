package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.artist.ArtistProfileDTO;
import com.project.cadence.dto.artist.NewArtistDTO;
import com.project.cadence.dto.artist.UpdateArtistDTO;
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
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/artist")
public class ArtistController {
    private final JwtService jwtService;
    private final ArtistService artistService;

    @PostMapping(path = "/admin/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewArtist(HttpServletRequest request, @Validated @RequestBody NewArtistDTO newArtistDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return artistService.addNewArtist(newArtistDTO);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @PutMapping(path = "/admin/update/{artistId}")
    public ResponseEntity<ApiResponseDTO<String>> updateExistingArtist(HttpServletRequest request, @Validated @RequestBody UpdateArtistDTO updateArtistDTO, @PathVariable("artistId") String artistId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return artistService.updateExistingArtist(updateArtistDTO, artistId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }


    @DeleteMapping(path = "/admin/delete/{artistId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingArtist(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return artistService.deleteExistingArtist(artistId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping(path = "/all")
    public ResponseEntity<ApiResponseDTO<List<ArtistPreviewDTO>>> getAllArtists(@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "5") int size) {
        return artistService.getAllArtists(page, size);
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
