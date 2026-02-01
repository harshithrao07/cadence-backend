package com.project.cadence.service;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.genre.GenrePreviewDTO;
import com.project.cadence.dto.playlist.PlaylistPreviewDTO;
import com.project.cadence.dto.playlist.UpsertPlaylistDTO;
import com.project.cadence.dto.record.RecordPreviewWithCoverImageDTO;
import com.project.cadence.dto.song.EachSongDTO;
import com.project.cadence.dto.user.UserPreviewDTO;
import com.project.cadence.model.*;
import com.project.cadence.repository.PlaylistRepository;
import com.project.cadence.repository.SongRepository;
import com.project.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {
    private final PlaylistRepository playlistRepository;
    private final UserRepository userRepository;
    private final SongRepository songRepository;

    public ResponseEntity<ApiResponseDTO<String>> upsertPlaylist(String email, UpsertPlaylistDTO upsertPlaylistDTO) {
        try {
            Playlist playlist;

            if (upsertPlaylistDTO.id().isPresent()) {
                playlist = playlistRepository.findByIdAndOwnerEmail(upsertPlaylistDTO.id().get(), email)
                        .orElseThrow(() -> new RuntimeException("Playlist not found or access denied"));

                if (playlist.isSystem()) throw new RuntimeException("You are not allowed to edit this playlist");
            } else {
                playlist = new Playlist();

                User user = userRepository.findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
                playlist.setOwner(user);
                playlist.setVisibility(PlaylistVisibility.PUBLIC);
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

    public ResponseEntity<ApiResponseDTO<Void>> removeSongFromPlaylist(String email, String playlistId, String songId) {
        try {
            Playlist playlist = playlistRepository.findByIdAndOwnerEmail(playlistId, email)
                    .orElseThrow(() -> new RuntimeException("Playlist not found or access denied"));

            Song song = songRepository.findById(songId)
                    .orElseThrow(() -> new RuntimeException("Song not found"));

            boolean removed = playlist.getSongs().remove(song);

            if (!removed) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponseDTO<>(false, "Song not present in playlist", null));
            }

            playlistRepository.save(playlist);

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Song removed from playlist", null)
            );
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> likePlaylist(String email, String playlistId) {
        try {
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            Playlist playlist = playlistRepository.findById(playlistId)
                    .orElseThrow(() -> new RuntimeException("Playlist not found"));

            user.getLikedPlaylists().add(playlist);
            userRepository.save(user);

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Playlist liked successfully", null)
            );
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<PlaylistPreviewDTO>> getPlaylist(
            String email,
            String playlistId
    ) {
        try {
            Playlist playlist = playlistRepository.findById(playlistId)
                    .orElseThrow(() -> new RuntimeException("Playlist not found"));

            boolean isOwner = playlist.getOwner().getEmail().equals(email);

            if (playlist.getVisibility() == PlaylistVisibility.PRIVATE && !isOwner) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponseDTO<>(false, "Access denied", null));
            }

            PlaylistPreviewDTO dto = new PlaylistPreviewDTO(
                    playlist.getId(),
                    playlist.getName(),
                    playlist.getCoverUrl(),
                    new UserPreviewDTO(
                            playlist.getOwner().getId(),
                            playlist.getOwner().getName(),
                            playlist.getOwner().getProfileUrl()
                    ),
                    playlist.getVisibility(),
                    playlist.isSystem(),
                    playlist.getCreatedAt(),
                    playlist.getUpdatedAt()
            );

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Playlist fetched successfully", dto)
            );

        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<List<EachSongDTO>>> getSongsFromPlaylist(
            String email,
            String playlistId
    ) {
        try {
            Playlist playlist = playlistRepository.findById(playlistId)
                    .orElseThrow(() -> new RuntimeException("Playlist not found"));

            boolean isOwner = playlist.getOwner().getEmail().equals(email);

            if (playlist.getVisibility() == PlaylistVisibility.PRIVATE && !isOwner) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponseDTO<>(false, "Access denied", null));
            }

            List<Song> orderedSongs = playlist.getSongs();

            if (playlist.isSystem()
                    && playlist.getSystemType() == SystemPlaylistType.LIKED_SONGS) {

                orderedSongs = new ArrayList<>(orderedSongs);
                Collections.reverse(orderedSongs);
            }

            List<EachSongDTO> songs = orderedSongs.stream().map(song -> new EachSongDTO(
                            song.getId(),
                            song.getTitle(),
                            song.getTotalDuration(),
                            song.getCreatedBy().stream()
                                    .map(artist -> new ArtistPreviewDTO(
                                            artist.getId(),
                                            artist.getName(),
                                            artist.getProfileUrl()
                                    ))
                                    .toList(),
                            song.getGenres().stream()
                                    .map(genre -> new GenrePreviewDTO(
                                            genre.getId(),
                                            genre.getType()
                                    ))
                                    .toList(),
                            new RecordPreviewWithCoverImageDTO(
                                    song.getRecord().getId(),
                                    song.getRecord().getTitle(),
                                    song.getRecord().getCoverUrl(),
                                    song.getRecord().getReleaseTimestamp()
                            )
                    ))
                    .toList();

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Playlist songs fetched successfully", songs)
            );

        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> deletePlaylist(
            String email,
            String playlistId
    ) {
        try {
            Playlist playlist = playlistRepository.findById(playlistId)
                    .orElseThrow(() -> new RuntimeException("Playlist not found"));

            // ownership check
            if (!playlist.getOwner().getEmail().equals(email)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponseDTO<>(false, "Access denied", null));
            }

            // system playlist protection
            if (playlist.isSystem()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(new ApiResponseDTO<>(false, "System playlists cannot be deleted", null));
            }

            playlistRepository.delete(playlist);

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Playlist deleted successfully", null)
            );

        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<List<PlaylistPreviewDTO>>> getAllPlaylists(
            String email
    ) {
        try {
            List<Playlist> playlists =
                    playlistRepository
                            .findAllByOwner_EmailAndIsSystemFalseOrderByCreatedAtDesc(email);

            List<PlaylistPreviewDTO> result = playlists.stream()
                    .map(playlist -> new PlaylistPreviewDTO(
                            playlist.getId(),
                            playlist.getName(),
                            playlist.getCoverUrl(),
                            new UserPreviewDTO(
                                    playlist.getOwner().getId(),
                                    playlist.getOwner().getName(),
                                    playlist.getOwner().getProfileUrl()
                            ),
                            playlist.getVisibility(),
                            playlist.isSystem(),
                            playlist.getCreatedAt(),
                            playlist.getUpdatedAt()
                    ))
                    .toList();

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Playlists fetched successfully", result)
            );

        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public List<PlaylistPreviewDTO> getPlaylistsForSearch(Pageable pageable, String key) {
        try {
            Page<Playlist> playlistPage =
                    playlistRepository.findByVisibilityAndNameContainingIgnoreCase(
                            PlaylistVisibility.PUBLIC,
                            key == null ? "" : key,
                            pageable
                    );

            return playlistPage.getContent()
                    .stream()
                    .map(playlist -> new PlaylistPreviewDTO(
                                    playlist.getId(),
                                    playlist.getName(),
                                    playlist.getCoverUrl(),
                                    new UserPreviewDTO(
                                            playlist.getOwner().getId(),
                                            playlist.getOwner().getName(),
                                            playlist.getOwner().getProfileUrl()
                                    ),
                                    playlist.getVisibility(),
                                    playlist.isSystem(),
                                    playlist.getCreatedAt(),
                                    playlist.getUpdatedAt()
                            )
                    )
                    .toList();
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return List.of();
        }
    }
}
