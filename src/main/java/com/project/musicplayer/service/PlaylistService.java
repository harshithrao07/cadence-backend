package com.project.musicplayer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.musicplayer.dto.playlist.PlaylistPreviewDTO;
import com.project.musicplayer.model.Playlist;
import com.project.musicplayer.repository.PlaylistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    public Set<PlaylistPreviewDTO> getCreatedPlaylistInfo(String userId) {
        Set<Optional<Playlist>> playlists = playlistRepository.findByUserId(userId);
        if (playlists == null) {
            return null;
        }

        Set<PlaylistPreviewDTO> playlistPreviewDTOS = new HashSet<>();
        for (Optional<Playlist> playlist : playlists) {
            playlistPreviewDTOS.add(objectMapper.convertValue(playlist, PlaylistPreviewDTO.class));
        }

        return playlistPreviewDTOS;
    }
}