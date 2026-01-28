package com.project.cadence.service;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.PaginatedResponseDTO;
import com.project.cadence.dto.artist.*;
import com.project.cadence.dto.record.RecordPreviewDTO;
import com.project.cadence.dto.song.TopSongsInArtistProfileDTO;
import com.project.cadence.dto.user.UserPreviewDTO;
import com.project.cadence.model.*;
import com.project.cadence.repository.ArtistRepository;
import com.project.cadence.repository.RecordRepository;
import com.project.cadence.repository.SongRepository;
import com.project.cadence.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ArtistService {
    private final UserRepository userRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final RecordRepository recordRepository;
    private final AwsService awsService;
    private final JdbcTemplate jdbcTemplate;

    public ResponseEntity<ApiResponseDTO<String>> upsertArtist(UpsertArtistDTO dto) {
        try {
            Artist artist;
            if (dto.id().isPresent()) {
                artist = artistRepository.findById(dto.id().get())
                        .orElseThrow(() -> new RuntimeException("Artist not found"));

                artist.setName(dto.name());
                artist.setDescription(dto.description().orElse(null));

            } else {
                artist = Artist.builder()
                        .name(dto.name())
                        .description(dto.description().orElse(null))
                        .build();
            }

            Artist savedArtist = artistRepository.save(artist);
            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true, "Artist created successfully", savedArtist.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingArtist(String artistId) {
        try {
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            jdbcTemplate.update(
                    "DELETE FROM artist_created_songs WHERE artist_id = ?",
                    artistId
            );

            jdbcTemplate.update(
                    "DELETE FROM artist_records WHERE artist_id = ?",
                    artistId
            );

            String coverUrl = artist.getProfileUrl();
            artistRepository.delete(artist);

            if (coverUrl != null) {
                String key = awsService.extractKeyFromUrl(coverUrl);
                if (awsService.findByName(key)) {
                    awsService.deleteObject(key);
                }
            }

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Successfully deleted artist", null)
            );

        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error has occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<PaginatedResponseDTO<ArtistPreviewDTO>>> getAllArtists(
            int page,
            int size,
            String key
    ) {
        try {
            PageRequest pageable = PageRequest.of(page, size, Sort.by("name").ascending());

            Page<Artist> artistPage;
            if (key != null && !key.trim().isEmpty()) {
                artistPage = artistRepository.findByNameStartingWithIgnoreCase(key.trim(), pageable);
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

            PaginatedResponseDTO<ArtistPreviewDTO> response =
                    new PaginatedResponseDTO<>(
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

    public ResponseEntity<ApiResponseDTO<ArtistProfileDTO>> getArtistProfile(String artistId) {
        try {
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            // Fetch 5 latest records
            Set<RecordPreviewDTO> recordPreviewDTOS = new HashSet<>();
            recordRepository.findByArtistsOrderByReleaseTimestampDesc(artist, PageRequest.of(0, 5)).forEach(record -> recordPreviewDTOS.add(new RecordPreviewDTO(
                    record.getId(),
                    record.getTitle(),
                    record.getReleaseTimestamp(),
                    record.getCoverUrl(),
                    record.getRecordType(),
                    record.getArtists().stream()
                            .map(artist1 -> new ArtistPreviewDTO(
                                    artist1.getId(),
                                    artist1.getName(),
                                    artist1.getProfileUrl()
                            )).toList()
            )));

            // Fetch 10 popular songs
            Page<TopSongsInArtistProfileDTO> popularSongs = songRepository.findTopSongsForArtist(artistId, PageRequest.of(0, 10));
            for (TopSongsInArtistProfileDTO song : popularSongs) {
                List<Artist> createdBy = songRepository.findCreatorsBySongId(song.id());

                List<ArtistPreviewDTO> artistPreviewDTOS = createdBy.stream()
                        .map(a -> new ArtistPreviewDTO(
                                a.getId(),
                                a.getName(),
                                a.getProfileUrl()
                        ))
                        .toList();

                song.artists().addAll(artistPreviewDTOS);
            }

            // Get Monthly listeners
            Instant fromDate = Instant.now().minus(30, ChronoUnit.DAYS);
            Long monthlyListeners = artistRepository.getTotalListenersForGivenDuration(artistId, fromDate);

            return ResponseEntity.status(HttpStatus.OK).body(
                    new ApiResponseDTO<>(
                            true,
                            "Successfully retrieved the artist profile",
                            new ArtistProfileDTO(
                                    artist.getId(),
                                    artist.getName(),
                                    artist.getProfileUrl(),
                                    artist.getDescription(),
                                    artist.getArtistFollowers().size(),
                                    monthlyListeners,
                                    popularSongs.stream().toList(),
                                    recordPreviewDTOS
                            )
                    )
            );
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
            userRepository.save(user);
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
            userRepository.save(user);
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

    public ResponseEntity<ApiResponseDTO<List<UserPreviewDTO>>> getArtistFollowers(String artistId) {
        try {
            Artist artist = artistRepository.findById(artistId).orElse(null);
            if (artist == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Artist not found", null));
            }

            List<UserPreviewDTO> userPreviewDTOS = new ArrayList<>();
            artist.getArtistFollowers().forEach(follower -> userPreviewDTOS.add(
                    new UserPreviewDTO(
                            follower.getId(),
                            follower.getName(),
                            follower.getProfileUrl()
                    )
            ));
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved artist followers", userPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error has occurred in the server", null));
        }
    }
}
