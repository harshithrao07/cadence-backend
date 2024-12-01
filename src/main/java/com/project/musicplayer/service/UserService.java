package com.project.musicplayer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.artist.ArtistPreviewDTO;
import com.project.musicplayer.dto.genre.GenrePreviewDTO;
import com.project.musicplayer.dto.playlist.PlaylistPreviewDTO;
import com.project.musicplayer.dto.song.TrackPreviewDTO;
import com.project.musicplayer.dto.user.UserProfileChangeDTO;
import com.project.musicplayer.dto.user.UserProfileDTO;
import com.project.musicplayer.dto.user.UserPreviewDTO;
import com.project.musicplayer.model.*;
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
    ObjectMapper objectMapper = new ObjectMapper();

    public Optional<User> loadUserByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    private boolean authenticatedUserMatchesProfileLookup(String tokenEmail, String userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.isPresent() && user.get().getEmail().equals(tokenEmail);
    }

    public ResponseEntity<ApiResponseDTO<UserProfileDTO>> getUserProfile(String userId, String tokenEmail) {
        try {
            // Whether the token email matches with the requested profile's email
            boolean matches = authenticatedUserMatchesProfileLookup(tokenEmail, userId);

            Optional<User> user = userRepository.findById(userId);
            if (user.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "User not found", null));
            }

            Set<GenrePreviewDTO> genrePreviewDTOS = new HashSet<>();
            Set<Genre> genres = user.get().getGenrePreferences();
            for (Genre genre : genres) {
                genrePreviewDTOS.add(objectMapper.convertValue(genre, GenrePreviewDTO.class));
            }


            PlaylistVisibility playlistVisibility = matches ? null : PlaylistVisibility.PUBLIC;
            Set<PlaylistPreviewDTO> createdPlaylistsPreviewDTOs = new HashSet<>();
            Set<Playlist> createdPlaylists = user.get().getPlaylists();
            for (Playlist playlist : createdPlaylists) {
                if (playlistVisibility == null) {
                    createdPlaylistsPreviewDTOs.add(objectMapper.convertValue(playlist, PlaylistPreviewDTO.class));
                } else if (playlist.getVisibility() == playlistVisibility) {
                    createdPlaylistsPreviewDTOs.add(objectMapper.convertValue(playlist, PlaylistPreviewDTO.class));
                }
            }

            Set<PlaylistPreviewDTO> likedPlaylistsPreviewDTOs = new HashSet<>();
            if (matches) {
                Set<Playlist> likedPlaylists = user.get().getLikedPlaylists();
                for (Playlist playlist : likedPlaylists) {
                    likedPlaylistsPreviewDTOs.add(objectMapper.convertValue(playlist, PlaylistPreviewDTO.class));
                }
            }

            Set<TrackPreviewDTO> likedSongsPreviewDTOs = new HashSet<>();
            if (matches) {
                Set<Song> likedSongs = user.get().getLikedSongs();
                int count = 0;
                for (Song song : likedSongs) {
                    likedSongsPreviewDTOs.add(objectMapper.convertValue(song, TrackPreviewDTO.class));
                    count++;

                    if (count > 5) {
                        break;
                    }
                }
            }

            Set<UserPreviewDTO> userFollowersPreviewDTOs = new HashSet<>();
            Set<User> userFollowers = user.get().getUserFollowers();
            for (User follower : userFollowers) {
                userFollowersPreviewDTOs.add(objectMapper.convertValue(follower, UserPreviewDTO.class));
            }

            Set<UserPreviewDTO> userFollowingPreviewDTOs = new HashSet<>();
            Set<User> userFollowing = user.get().getUserFollowing();
            for (User following : userFollowing) {
                userFollowingPreviewDTOs.add(objectMapper.convertValue(following, UserPreviewDTO.class));
            }

            Set<ArtistPreviewDTO> artistFollowingPreviewDTOs = new HashSet<>();
            Set<Artist> artistFollowing = user.get().getArtistFollowing();
            for (Artist following : artistFollowing) {
                artistFollowingPreviewDTOs.add(objectMapper.convertValue(following, ArtistPreviewDTO.class));
            }

            UserProfileDTO userProfileDTO = new UserProfileDTO(
                    user.get().getId(),
                    user.get().getName(),
                    user.get().getEmail(),
                    user.get().getProfileUrl(),
                    genrePreviewDTOS,
                    createdPlaylistsPreviewDTOs,
                    likedPlaylistsPreviewDTOs,
                    likedSongsPreviewDTOs,
                    userFollowersPreviewDTOs,
                    userFollowingPreviewDTOs,
                    artistFollowingPreviewDTOs
            );

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved user profile", userProfileDTO));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> putUserProfile(String userId, UserProfileChangeDTO userProfileChangeDTO, String tokenEmail) {
        try {
            // Whether the token email matches with the requested profile's email if not then unauthorized
            if (!authenticatedUserMatchesProfileLookup(tokenEmail, userId)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "Not authorized to edit profile", null));
            }

            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "An error occurred: requesting user not found", null));
            }
            objectMapper.updateValue(user, userProfileChangeDTO);
            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "User profile updated successfully", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> followUser(String userId, String tokenEmail) {
        try {
            // Prevents the user from following themselves
            if (authenticatedUserMatchesProfileLookup(tokenEmail, userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "You cannot follow yourself", null));
            }

            // Find the user making the request
            User requestingUser = userRepository.findByEmail(tokenEmail).orElse(null);
            if (requestingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "An error occurred: requesting user not found", null));
            }

            // Check if the target user exists
            User targetUser = userRepository.findById(userId).orElse(null);
            if (targetUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "User not found", null));
            }

            if (requestingUser.getUserFollowing().contains(targetUser)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "You are already following this user", null));
            }

            requestingUser.getUserFollowing().add(targetUser);
            userRepository.save(requestingUser);

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully followed the user", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> unfollowUser(String userId, String tokenEmail) {
        try {
            // Prevents the user from unfollowing themselves
            if (authenticatedUserMatchesProfileLookup(tokenEmail, userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "You cannot unfollow yourself", null));
            }

            // Find the user making the request
            User requestingUser = userRepository.findByEmail(tokenEmail).orElse(null);
            if (requestingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "An error occurred: requesting user not found", null));
            }

            // Check if the target user exists
            User targetUser = userRepository.findById(userId).orElse(null);
            if (targetUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "User not found", null));
            }

            if (!requestingUser.getUserFollowing().contains(targetUser)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "You are not following this user", null));
            }

            requestingUser.getUserFollowing().remove(targetUser);
            userRepository.save(requestingUser);

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully unfollowed the user", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Boolean>> isFollowing(String userId, String tokenEmail) {
        try {
            if (authenticatedUserMatchesProfileLookup(tokenEmail, userId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "You cannot check whether you are following yourself", null));
            }

            // Find the user making the request
            User requestingUser = userRepository.findByEmail(tokenEmail).orElse(null);
            if (requestingUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Requesting user not found", null));
            }

            // Check if the target user exists
            User targetUser = userRepository.findById(userId).orElse(null);
            if (targetUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "User not found", null));
            }

            if (requestingUser.getUserFollowing().contains(targetUser)) {
                return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "You are following this user", true));
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "You are not following this user", false));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Set<UserPreviewDTO>>> getUserFollowers(String userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "User not found", null));
            }

            Set<UserPreviewDTO> userPreviewDTOS = new HashSet<>();
            Set<User> userFollowers = user.getUserFollowers();
            for (User userFollower : userFollowers) {
                userPreviewDTOS.add(objectMapper.convertValue(userFollower, UserPreviewDTO.class));
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved user followers", userPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Set<UserPreviewDTO>>> getUserFollowing(String userId) {
        try {
            User user = userRepository.findById(userId).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "User not found", null));
            }

            Set<UserPreviewDTO> userPreviewDTOS = new HashSet<>();
            Set<User> userFollowings = user.getUserFollowing();
            for (User userFollowing : userFollowings) {
                userPreviewDTOS.add(objectMapper.convertValue(userFollowing, UserPreviewDTO.class));
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved user following", userPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> logOut(@NotNull String token) {
        try {
            String[] sections = token.split("\\.");

            Base64.Decoder decoder = Base64.getUrlDecoder();
            String payload = new String(decoder.decode(sections[1]));

            String userEmail = jwtService.extractEmailForPayload(payload);
            InvalidatedToken invalidatedToken = jwtService.invalidateAllUserTokens(userEmail);
            if (invalidatedToken == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred while logging out", null));
            }
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Logged out successfully", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }
}
