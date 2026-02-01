package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.playlist.PlaylistPreviewDTO;
import com.project.cadence.dto.playlist.UpsertPlaylistDTO;
import com.project.cadence.dto.song.EachSongDTO;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.PlaylistService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/playlist")
public class PlaylistController {

    private final PlaylistService playlistService;
    private final JwtService jwtService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<List<PlaylistPreviewDTO>>> getAllPlaylists(
            HttpServletRequest request
    ) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.getAllPlaylists(email);
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponseDTO<String>> addNewPlaylist(
            HttpServletRequest request,
            @Validated @RequestBody UpsertPlaylistDTO upsertPlaylistDTO
    ) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.upsertPlaylist(email, upsertPlaylistDTO);
    }

    @PutMapping("/{playlistId}/song/{songId}")
    public ResponseEntity<ApiResponseDTO<Void>> addSongToPlaylist(
            HttpServletRequest request,
            @PathVariable String playlistId,
            @PathVariable String songId
    ) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.addSongToPlaylist(email, playlistId, songId);
    }

    @DeleteMapping("/{playlistId}/song/{songId}")
    public ResponseEntity<ApiResponseDTO<Void>> removeSongFromPlaylist(
            HttpServletRequest request,
            @PathVariable String playlistId,
            @PathVariable String songId
    ) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.removeSongFromPlaylist(email, playlistId, songId);
    }

    @PutMapping("/{playlistId}/like")
    public ResponseEntity<ApiResponseDTO<Void>> likePlaylist(
            HttpServletRequest request,
            @PathVariable String playlistId
    ) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.likePlaylist(email, playlistId);
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<ApiResponseDTO<PlaylistPreviewDTO>> getPlaylist(
            HttpServletRequest request,
            @PathVariable String playlistId
    ) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.getPlaylist(email, playlistId);
    }

    @GetMapping("/{playlistId}/songs")
    public ResponseEntity<ApiResponseDTO<List<EachSongDTO>>> getSongsFromPlaylist(
            HttpServletRequest request,
            @PathVariable String playlistId
    ) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.getSongsFromPlaylist(email, playlistId);
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<ApiResponseDTO<Void>> deletePlaylist(
            HttpServletRequest request,
            @PathVariable String playlistId
    ) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return playlistService.deletePlaylist(email, playlistId);
    }
}
