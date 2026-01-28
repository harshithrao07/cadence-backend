package com.project.cadence.service;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.playlist.UpsertPlaylistDTO;
import com.project.cadence.model.Playlist;
import com.project.cadence.model.Song;
import com.project.cadence.model.User;
import com.project.cadence.repository.PlaylistRepository;
import com.project.cadence.repository.SongRepository;
import com.project.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;

    public ResponseEntity<ApiResponseDTO<String>> addNewPlaylist(String email, UpsertPlaylistDTO upsertPlaylistDTO) {
        try {
            Playlist playlist;

            if (upsertPlaylistDTO.id().isPresent()) {
                playlist = playlistRepository.findByIdAndOwnerEmail(upsertPlaylistDTO.id().get(), email)
                        .orElseThrow(() -> new RuntimeException("Playlist not found or access denied"));
            } else {
                playlist = new Playlist();

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                playlist.setOwner(user);
            }

            playlist.setName(upsertPlaylistDTO.name());
            upsertPlaylistDTO.playlistVisibility().ifPresent(playlist::setVisibility);

            Playlist upsertedPlaylist = playlistRepository.save(playlist);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Successfully upserted playlist", upsertedPlaylist.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> addSongToPlaylist(String email, String playlistId, String songId) {
        try {
            Playlist playlist = playlistRepository.findByIdAndOwnerEmail(playlistId, email)
                    .orElseThrow(() -> new RuntimeException("Playlist not found or access denied"));

            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new RuntimeException("Song not found"));

            playlist.getSongs().add(song);
            playlistRepository.save(playlist);
            return ResponseEntity.ok(new ApiResponseDTO<>(true, "Successfully upserted playlist", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }
}
