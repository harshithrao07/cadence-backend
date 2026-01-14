package com.project.cadence.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.*;
import com.project.cadence.dto.record.RecordPreviewDTO;
import com.project.cadence.dto.song.SongsInArtistProfileDTO;
import com.project.cadence.dto.user.UserPreviewDTO;
import com.project.cadence.model.*;
import com.project.cadence.model.Record;
import com.project.cadence.repository.ArtistRepository;
import com.project.cadence.repository.SongRepository;
import com.project.cadence.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final AwsService awsService;
    ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<ApiResponseDTO<ArtistProfileDTO>> getArtistProfile(String artistId) {
        try {
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            Set<RecordPreviewDTO> recordPreviewDTOS = new HashSet<>();

            Set<Record> artistRecords = artist.getArtistRecords();

            // Fetch the 5 most recent records
            Set<Record> recentRecords = artistRecords.stream()
                    .sorted(Comparator.comparing(Record::getReleaseTimestamp).reversed())
                    .limit(5)
                    .collect(Collectors.toSet());
            recentRecords.forEach(record -> recordPreviewDTOS.add(new RecordPreviewDTO(
                    record.getId(),
                    record.getTitle(),
                    record.getReleaseTimestamp(),
                    record.getCoverUrl(),
                    record.getRecordType(),
                    new ArrayList<>()
            )));

            // Fetch popular songs
            List<SongsInArtistProfileDTO> popularSongs =
                    artistRepository.findTopSongsForArtist(artistId, PageRequest.of(0, 10));

            for (SongsInArtistProfileDTO song : popularSongs) {
                Set<Artist> createdBy = songRepository.findArtistsBySongId(song.songId());

                List<ArtistPreviewDTO> artistPreviewDTOS = createdBy.stream()
                        .map(a -> new ArtistPreviewDTO(
                                a.getId(),
                                a.getName(),
                                a.getProfileUrl()
                        ))
                        .toList();

                song.artists().addAll(artistPreviewDTOS);
            }

            Instant fromDate = Instant.now().minus(30, ChronoUnit.DAYS);

            Long monthlyListeners =
                    artistRepository.getMonthlyListenersForArtist(artistId, fromDate);


            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDTO<>(
                            true,
                            "Successfully retrieved the artist profile",
                            new ArtistProfileDTO(
                                    artist.getId(),
                                    artist.getName(),
                                    artist.getProfileUrl(),
                                    artist.getDescription(),
                                    artist.getFollowersCount(),
                                    monthlyListeners,
                                    popularSongs,
                                    recordPreviewDTOS
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
            if (newArtistDTO.name().isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist name cannot be empty", null));
            }

            if (artistRepository.existsByName(newArtistDTO.name())) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artist already exists", null));
            }

            Artist artist = Artist.builder()
                    .name(newArtistDTO.name())
                    .description(newArtistDTO.description().orElse(""))
                    .build();

            Artist savedArtist = artistRepository.save(artist);
            if (savedArtist.getId() == null) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred while creating the artist", null));
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true, "Artist created successfully", artist.getId()));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error has occurred in the server", null));
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
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error has occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<PaginatedAllArtistsResponse>> getAllArtists(
            int page,
            int size,
            String key
    ) {
        try {
            PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());

            Page<Artist> artistPage;

            if (key != null && !key.trim().isEmpty()) {
                artistPage = artistRepository.findByNameContainingIgnoreCase(key.trim(), pageable);
            } else {
                artistPage = artistRepository.findAll(pageable);
            }

            List<ArtistPreviewDTO> artistPreviewDTOS = artistPage
                    .stream()
                    .map(artist -> new ArtistPreviewDTO(
                            artist.getId(),
                            artist.getName(),
                            artist.getProfileUrl()
                    ))
                    .toList();

            PaginatedAllArtistsResponse response =
                    new PaginatedAllArtistsResponse(
                            artistPreviewDTOS,
                            artistPage.getNumber(),
                            artistPage.getSize(),
                            artistPage.getTotalElements(),
                            artistPage.getTotalPages(),
                            artistPage.isLast()
                    );
            return ResponseEntity.ok(
                    new ApiResponseDTO<>(
                            true,
                            "Successfully retrieved artists",
                            response
                    )
            );

        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error has occurred in the server", null));
        }
    }


    public ResponseEntity<ApiResponseDTO<String>> updateExistingArtist(UpdateArtistDTO updateArtistDTO, String artistId) {
        try {
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            if (updateArtistDTO.name() != null) {
                if (!updateArtistDTO.name().isBlank()) {
                    artist.setName(updateArtistDTO.name());
                } else {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artist name cannot be empty", null));
                }
            }

            if (updateArtistDTO.profileUrl() != null) {
                if (!updateArtistDTO.profileUrl().equals(artist.getProfileUrl()) && !updateArtistDTO.profileUrl().isEmpty()) {
                    if (!awsService.findByName(updateArtistDTO.profileUrl())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Updated " + artist.getName() + " profile has not been uploaded to S3 yet", null));
                    } else {
                        if (awsService.findByName(artist.getProfileUrl())) {
                            awsService.deleteObject(artist.getProfileUrl());
                        }

                        artist.setProfileUrl(updateArtistDTO.profileUrl());
                    }
                }
            }

            if (updateArtistDTO.description() != null) {
                artist.setDescription(updateArtistDTO.description());
            }

            Artist updatedArtist = artistRepository.save(artist);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully updated artist", updatedArtist.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error has occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingArtist(String artistId) {
        try {
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            if (awsService.findByName(artist.getProfileUrl())) {
                awsService.deleteObject(artist.getProfileUrl());
            }

            Set<Record> records = artist.getArtistRecords();
            records.forEach(record -> {
                if (awsService.findByName(record.getCoverUrl())) {
                    awsService.deleteObject(record.getCoverUrl());
                }
            });

            Set<Song> songs = artist.getArtistSongs();
            songs.forEach(song -> {
                if (awsService.findByName(song.getSongUrl())) {
                    awsService.deleteObject(song.getSongUrl());
                }
            });

            artistRepository.deleteById(artistId);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully deleted artist", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error has occurred in the server", null));
        }
    }
}
