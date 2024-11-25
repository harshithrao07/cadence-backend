package com.project.musicplayer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.musicplayer.dto.artist.ArtistIsFollowingDTO;
import com.project.musicplayer.dto.user.UserPreviewDTO;
import com.project.musicplayer.model.Artist;
import com.project.musicplayer.model.User;
import com.project.musicplayer.repository.ArtistRepository;
import com.project.musicplayer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<String> followArtist(String artistId, String tokenEmail) {
        try {
            User user = userRepository.findByEmail(tokenEmail).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("An error occurred: requesting user not found");
            }

            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Artist not found");
            }

            if (user.getArtistFollowing().contains(artist)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You are already following this artist");
            }

            user.getArtistFollowing().add(artist);
            return ResponseEntity.status(HttpStatus.OK).body("Successfully followed the artist");
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred in the server");
        }
    }

    public ResponseEntity<String> unfollowArtist(String artistId, String tokenEmail) {
        try {
            User user = userRepository.findByEmail(tokenEmail).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("An error occurred: requesting user not found");
            }

            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Artist not found");
            }

            if (!user.getArtistFollowing().contains(artist)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You are not following this artist");
            }

            user.getArtistFollowing().remove(artist);
            return ResponseEntity.status(HttpStatus.OK).body("Successfully unfollowed the artist");
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred in the server");
        }
    }

    public ResponseEntity<ArtistIsFollowingDTO> isFollowing(String artistId, String tokenEmail) {
        try {
            // Find the user making the request
            User user = userRepository.findByEmail(tokenEmail).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            // Check if the target user exists
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            if (user.getArtistFollowing().contains(artist)) {
                return ResponseEntity.status(HttpStatus.OK).body(new ArtistIsFollowingDTO(true));
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ArtistIsFollowingDTO(false));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<Set<UserPreviewDTO>> getArtistFollowers(String artistid, String tokenEmail) {
        try {
            User currentUser = userRepository.findByEmail(tokenEmail).orElse(null);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Artist artist = artistRepository.findById(artistid).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            Set<UserPreviewDTO> userPreviewDTOS = new HashSet<>();
            Set<User> userFollowing = currentUser.getUserFollowing();
            for (User user : userFollowing) {
                if (user.getArtistFollowing().contains(artist)) {
                    userPreviewDTOS.add(objectMapper.convertValue(user, UserPreviewDTO.class));
                }
            }

            return ResponseEntity.status(HttpStatus.OK).body(userPreviewDTOS);
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
