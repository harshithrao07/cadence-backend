package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.playlist.PlaylistPreviewDTO;
import com.project.cadence.dto.playlist.UpsertPlaylistDTO;
import com.project.cadence.dto.song.EachSongDTO;
import com.project.cadence.service.PlaylistService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/playlist")
public class PlaylistController {

    private final PlaylistService playlistService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<List<PlaylistPreviewDTO>>> getAllPlaylists(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        return playlistService.getAllPlaylists(userDetails.getUsername());
    }

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponseDTO<String>> addNewPlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @Validated @RequestBody UpsertPlaylistDTO upsertPlaylistDTO
    ) {
        return playlistService.upsertPlaylist(userDetails.getUsername(), upsertPlaylistDTO);
    }

    @PutMapping("/{playlistId}/song/{songId}")
    public ResponseEntity<ApiResponseDTO<Void>> addSongToPlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String playlistId,
            @PathVariable String songId
    ) {
        return playlistService.addSongToPlaylist(userDetails.getUsername(), playlistId, songId);
    }

    @DeleteMapping("/{playlistId}/song/{songId}")
    public ResponseEntity<ApiResponseDTO<Void>> removeSongFromPlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String playlistId,
            @PathVariable String songId
    ) {
        return playlistService.removeSongFromPlaylist(userDetails.getUsername(), playlistId, songId);
    }

    @PutMapping("/{playlistId}/like")
    public ResponseEntity<ApiResponseDTO<Void>> likePlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String playlistId
    ) {
        return playlistService.likePlaylist(userDetails.getUsername(), playlistId);
    }

    @GetMapping("/{playlistId}")
    public ResponseEntity<ApiResponseDTO<PlaylistPreviewDTO>> getPlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String playlistId
    ) {
        return playlistService.getPlaylist(userDetails.getUsername(), playlistId);
    }

    @GetMapping("/{playlistId}/songs")
    public ResponseEntity<ApiResponseDTO<List<EachSongDTO>>> getSongsFromPlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String playlistId
    ) {
        return playlistService.getSongsFromPlaylist(userDetails.getUsername(), playlistId);
    }

    @DeleteMapping("/{playlistId}")
    public ResponseEntity<ApiResponseDTO<Void>> deletePlaylist(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String playlistId
    ) {
        return playlistService.deletePlaylist(userDetails.getUsername(), playlistId);
    }
}
