package com.project.musicplayer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.artist.ArtistPreviewDTO;
import com.project.musicplayer.dto.artist.ArtistProfileDTO;
import com.project.musicplayer.dto.artist.NewArtistDTO;
import com.project.musicplayer.dto.artist.UpdateArtistDTO;
import com.project.musicplayer.dto.genre.GenrePreviewDTO;
import com.project.musicplayer.dto.releases.ReleasesPreviewDTO;
import com.project.musicplayer.dto.song.TrackPreviewDTO;
import com.project.musicplayer.dto.user.UserPreviewDTO;
import com.project.musicplayer.model.*;
import com.project.musicplayer.repository.ArtistRepository;
import com.project.musicplayer.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<ApiResponseDTO<ArtistProfileDTO>> getArtistProfile(String artistId) {
        try {
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            Set<TrackPreviewDTO> trackPreviewDTOS = new HashSet<>();
            Set<ReleasesPreviewDTO> releasesPreviewDTOS = new HashSet<>();

            Set<Releases> artistReleases = artist.getArtistReleases();

            // Fetch the 5 most recent releases
            Set<Releases> recentReleases = artistReleases.stream()
                    .sorted(Comparator.comparing(Releases::getReleaseTimestamp).reversed())
                    .limit(5)
                    .collect(Collectors.toSet());

            // Fetch 5 songs from the most recent releases
            Set<Song> recentCreatedSongs = recentReleases.stream()
                    .flatMap(release -> release.getSongs().stream().limit(5))
                    .collect(Collectors.toSet());

            recentCreatedSongs.forEach(song -> trackPreviewDTOS.add(objectMapper.convertValue(song, TrackPreviewDTO.class)));
            recentReleases.forEach(release -> releasesPreviewDTOS.add(objectMapper.convertValue(release, ReleasesPreviewDTO.class)));

            Set<Song> createdSongs = artist.getCreatedSongs();

            Map<GenrePreviewDTO, Long> genreUsedPreviewDTO = new HashMap<>();
            Set<ArtistProfileDTO> relatedArtistsProfileDTOs = new HashSet<>();
            if (!createdSongs.isEmpty()) {
                // Map of genre most used by the artist
                Map<Genre, Long> genreUsed = createdSongs.stream()
                        .flatMap(song -> song.getGenres().stream())
                        .collect(Collectors.groupingBy(genre -> genre, Collectors.counting()));

                genreUsedPreviewDTO = genreUsed.entrySet().stream()
                        .collect(Collectors.toMap(
                                entry -> new GenrePreviewDTO(entry.getKey().getId(), entry.getKey().getType()),
                                Map.Entry::getValue
                        ));

                // Set of genre most used by the artist
                Long maxUsed = genreUsed.values().stream().max(Long::compare).orElse(0L);
                Set<Genre> mostUsedGenre = genreUsed.entrySet()
                        .stream()
                        .filter(entry -> Objects.equals(entry.getValue(), maxUsed))
                        .map(Map.Entry::getKey)
                        .collect(Collectors.toSet());

                // Get 5 related artists
                Set<Artist> relatedArtists = mostUsedGenre.stream()
                        .flatMap(genre -> genre.getSongs()
                                .stream()
                                .flatMap(song -> song.getArtists().stream()))
                        .limit(5)
                        .collect(Collectors.toSet());

                relatedArtists.forEach(relatedArtist -> relatedArtistsProfileDTOs.add(objectMapper.convertValue(relatedArtist, ArtistProfileDTO.class)));
            }

            Set<ReleasesPreviewDTO> featureReleasesPreviewDTOs = new HashSet<>();
            Set<Releases> featureReleases = artistRepository.findFeatureReleasesByArtistId(artistId);
            featureReleases.forEach(releases -> featureReleasesPreviewDTOs.add(objectMapper.convertValue(releases, ReleasesPreviewDTO.class)));

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDTO<>(
                            true,
                            "Successfully retrieved the artist profile",
                            new ArtistProfileDTO(
                                    artist.getId(),
                                    artist.getName(),
                                    artist.getProfileUrl(),
                                    artist.getDescription(),
                                    trackPreviewDTOS,
                                    releasesPreviewDTOS,
                                    relatedArtistsProfileDTOs,
                                    featureReleasesPreviewDTOs,
                                    genreUsedPreviewDTO
                            )
                    )
            );
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<String>> addNewArtist(NewArtistDTO newArtistDTO) {
        try {
            if (artistRepository.existsByName(newArtistDTO.name())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artist already exists", null));
            }

            if (newArtistDTO.name().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist name cannot be empty", null));
            }

            Artist artist = Artist.builder()
                    .name(newArtistDTO.name())
                    .profileUrl(newArtistDTO.profileUrl())
                    .description(newArtistDTO.description())
                    .build();

            Artist savedArtist = artistRepository.save(artist);
            if (savedArtist.getId() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred while creating the artist", null));
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Artist created successfully", artist.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> followArtist(String artistId, String tokenEmail) {
        try {
            User user = userRepository.findByEmail(tokenEmail).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "An error occurred: requesting user not found", null));
            }

            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            if (user.getArtistFollowing().contains(artist)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "You are already following this artist", null));
            }

            user.getArtistFollowing().add(artist);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully followed the artist", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> unfollowArtist(String artistId, String tokenEmail) {
        try {
            User user = userRepository.findByEmail(tokenEmail).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "An error occurred: requesting user not found", null));
            }

            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            if (!user.getArtistFollowing().contains(artist)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "You are not following this artist", null));
            }

            user.getArtistFollowing().remove(artist);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully unfollowed the artist", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Boolean>> isFollowing(String artistId, String tokenEmail) {
        try {
            // Find the user making the request
            User user = userRepository.findByEmail(tokenEmail).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "An error occurred: requesting user not found", null));
            }

            // Check if the target user exists
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            if (user.getArtistFollowing().contains(artist)) {
                return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "You are following this artist", true));
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "You are not following this artist", false));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<ApiResponseDTO<Set<UserPreviewDTO>>> getArtistFollowers(String artistId, String tokenEmail) {
        try {
            User currentUser = userRepository.findByEmail(tokenEmail).orElse(null);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "An error occurred: requesting user not found", null));
            }

            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            Set<UserPreviewDTO> userPreviewDTOS = new HashSet<>();
            Set<User> userFollowing = currentUser.getUserFollowing();
            for (User user : userFollowing) {
                if (user.getArtistFollowing().contains(artist)) {
                    userPreviewDTOS.add(objectMapper.convertValue(user, UserPreviewDTO.class));
                }
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved artist followers", userPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<ApiResponseDTO<Set<ArtistPreviewDTO>>> getAllArtists() {
        try {
            Set<Artist> artists = (Set<Artist>) artistRepository.findAll();

            Set<ArtistPreviewDTO> artistPreviewDTOS = new HashSet<>();
            artists.forEach(artist -> artistPreviewDTOS.add(objectMapper.convertValue(artist, ArtistPreviewDTO.class)));

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved all artists", artistPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<ApiResponseDTO<String>> updateExistingArtist(UpdateArtistDTO updateArtistDTO, String artistId) {
        try {
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            if (!updateArtistDTO.name().isEmpty()) {
                artist.setName(updateArtistDTO.name());
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist name cannot be empty", null));
            }

            artist.setProfileUrl(updateArtistDTO.profileUrl());
            artist.setDescription(updateArtistDTO.description());

            Artist updatedArtist = artistRepository.save(artist);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully updated artist", updatedArtist.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingArtist(String artistId) {
        try {
            if (!artistRepository.existsById(artistId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            artistRepository.deleteById(artistId);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully deleted artist", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
}
