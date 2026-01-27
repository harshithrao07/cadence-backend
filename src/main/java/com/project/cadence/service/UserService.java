package com.project.cadence.service;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.playlist.PlaylistPreviewDTO;
import com.project.cadence.dto.user.UserProfileChangeDTO;
import com.project.cadence.dto.user.UserProfileDTO;
import com.project.cadence.dto.user.UserPreviewDTO;
import com.project.cadence.model.*;
import com.project.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;

    public Optional<User> loadUserByEmail(String email) {
        return this.userRepository.findByEmail(email);
    }

    private boolean authenticatedUserMatchesProfileLookup(String tokenEmail, String userId) {
        Optional<User> user = userRepository.findById(userId);
        return user.isPresent() && user.get().getEmail().equals(tokenEmail);
    }

    public ResponseEntity<ApiResponseDTO<UserProfileDTO>> getUserProfile(
            String userId,
            String tokenEmail
    ) {
        try {
            // same endpoint for both owner and visitor

            boolean matches = authenticatedUserMatchesProfileLookup(tokenEmail, userId);
            User user = userRepository.findById(userId)
                    .orElse(null);

            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "User not found", null));
            }

            List<Playlist> createdPlaylists = user.getCreatedPlaylists();
            List<PlaylistPreviewDTO> createdPlaylistsPreview = createdPlaylists.stream()
                    .filter(playlist -> matches || playlist.getVisibility() != PlaylistVisibility.PRIVATE)
                    .sorted(Comparator.comparing(Playlist::getUpdatedAt).reversed())
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
                    )).toList();

            List<PlaylistPreviewDTO> likedPlaylistsPreview = new ArrayList<>(); // for owner only
            if (matches) {
                user.getLikedPlaylists()
                        .forEach(p ->
                                likedPlaylistsPreview.add(
                                        new PlaylistPreviewDTO(
                                                p.getId(),
                                                p.getName(),
                                                p.getCoverUrl(),
                                                new UserPreviewDTO(
                                                        p.getOwner().getId(),
                                                        p.getOwner().getName(),
                                                        p.getOwner().getProfileUrl()
                                                ),
                                                p.getVisibility(),
                                                p.isSystem(),
                                                p.getCreatedAt(),
                                                p.getUpdatedAt()
                                        )
                                )
                        );
            }

            List<ArtistPreviewDTO> artistFollowingPreview = new ArrayList<>();
            user.getArtistFollowing().forEach(artist ->
                    artistFollowingPreview.add(
                            new ArtistPreviewDTO(
                                    artist.getId(),
                                    artist.getName(),
                                    artist.getProfileUrl()
                            )
                    )
            );

            UserProfileDTO userProfileDTO = new UserProfileDTO(
                    user.getId(),
                    user.getName(),
                    matches ? user.getEmail() : null,
                    user.getProfileUrl(),
                    createdPlaylistsPreview,
                    likedPlaylistsPreview,
                    artistFollowingPreview
            );

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(
                            true,
                            "Successfully retrieved user profile",
                            userProfileDTO
                    )
            );

        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(
                            false,
                            "An error occurred in the server",
                            null
                    ));
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

            if (userProfileChangeDTO.name().isPresent() && !userProfileChangeDTO.name().get().isEmpty()) {
                user.setName(userProfileChangeDTO.name().get());
            }

            userRepository.save(user);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "User profile updated successfully", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public Role getUserRoleFromRepository(String email) {
        return userRepository.getRoleByEmail(email);
    }

}
