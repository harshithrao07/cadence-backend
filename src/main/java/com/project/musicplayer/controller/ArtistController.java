package com.project.musicplayer.controller;

import com.project.musicplayer.dto.artist.ArtistIsFollowingDTO;
import com.project.musicplayer.dto.artist.ArtistPreviewDTO;
import com.project.musicplayer.dto.user.UserIsFollowingDTO;
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

    @PostMapping(path = "/{artistId}/follow")
    public ResponseEntity<String> followArtist(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.followArtist(artistId, tokenEmail);
    }

    @PostMapping(path = "/{artistId}/unfollow")
    public ResponseEntity<String> unfollowArtist(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.unfollowArtist(artistId, tokenEmail);
    }

    @GetMapping(path = "/{artistId}/is_following")
    public ResponseEntity<ArtistIsFollowingDTO> isFollowing(HttpServletRequest request, @PathVariable("artistId") String artistId) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.isFollowing(artistId, tokenEmail);
    }

    @GetMapping(path = "/{artistId}/followers")
    public ResponseEntity<Set<UserPreviewDTO>> getArtistFollowers(HttpServletRequest request, @PathVariable("artistId") String artistid) {
        String tokenEmail = jwtService.getEmailFromHttpRequest(request);
        return artistService.getArtistFollowers(artistid, tokenEmail);
    }
}
