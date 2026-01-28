package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.playlist.UpsertPlaylistDTO;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.PlaylistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/playlist")
public class PlaylistController {
    private final PlaylistService playlistService;
    private final JwtService jwtService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewPlaylist(HttpServletRequest request, @Validated @RequestBody UpsertPlaylistDTO upsertPlaylistDTO) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.addNewPlaylist(email, upsertPlaylistDTO);
    }


    @PostMapping("/{playlistId}/song/{songId}")
    public ResponseEntity<ApiResponseDTO<Void>> addSongToPlaylist(HttpServletRequest request, @RequestParam String playlistId, @RequestParam String songId) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.addSongToPlaylist(email, playlistId, songId);
    }
}
