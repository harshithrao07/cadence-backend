package com.project.musicplayer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.musicplayer.dto.playlist.PlaylistPreviewDTO;
import com.project.musicplayer.dto.song.TrackPreviewDTO;
import com.project.musicplayer.dto.user.CurrentUserProfileDTO;
import com.project.musicplayer.model.InvalidatedToken;
import com.project.musicplayer.model.Playlist;
import com.project.musicplayer.model.Song;
import com.project.musicplayer.model.User;
import com.project.musicplayer.repository.PlaylistRepository;
import com.project.musicplayer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PlaylistService playlistService;

    ObjectMapper objectMapper = new ObjectMapper();

    public Optional<User> loadUserByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    public ResponseEntity<String> logOut(@NotNull String token) {
        try {
            String[] sections = token.split("\\.");

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(sections[1]));

            String userEmail = jwtService.extractEmailForPayload(payload);
            InvalidatedToken invalidatedToken = jwtService.invalidateAllUserTokens(userEmail);
            if (invalidatedToken == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred while logging out");
            }
            return ResponseEntity.status(HttpStatus.OK).body("Logged out successfully");
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred in the server");
        }
    }

    public ResponseEntity<CurrentUserProfileDTO> getUserProfile(String userId) {
        try {
            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Set<PlaylistPreviewDTO> createdPlaylistsPreviewDTOs = playlistService.getCreatedPlaylistInfo(userId);
            if (createdPlaylistsPreviewDTOs == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
            }

            Set<PlaylistPreviewDTO> likedPlaylistsPreviewDTOs = new HashSet<>();
            Set<Playlist> likedPlaylists = user.get().getLikedPlaylists();
            for (Playlist playlist : likedPlaylists) {
                likedPlaylistsPreviewDTOs.add(objectMapper.convertValue(playlist, PlaylistPreviewDTO.class));
            }

            Set<TrackPreviewDTO> likedSongsPreview = new HashSet<>();
            Set<Song> likedSongs = user.get().getLikedSongs();

            CurrentUserProfileDTO currentUserProfileDTO = new CurrentUserProfileDTO(
                    user.get().getId(),
                    user.get().getName(),
                    user.get().getEmail(),
                    user.get().getProfileUrl(),
                    createdPlaylistsPreviewDTOs,
                    likedPlaylistsPreviewDTOs,

                    )
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public boolean isAuthorizedToAccessProfile(String tokenEmail, String userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.isPresent() && user.get().getEmail().equals(tokenEmail);
    }
}
