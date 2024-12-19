package com.project.musicplayer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.releases.NewReleaseDTO;
import com.project.musicplayer.dto.releases.ReleasesPreviewDTO;
import com.project.musicplayer.dto.releases.UpdateReleaseDTO;
import com.project.musicplayer.model.Artist;
import com.project.musicplayer.model.ReleaseType;
import com.project.musicplayer.model.Releases;
import com.project.musicplayer.model.Song;
import com.project.musicplayer.repository.ArtistRepository;
import com.project.musicplayer.repository.ReleaseRepository;
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
public class ReleaseService {
    private final ReleaseRepository releaseRepository;
    private final ArtistRepository artistRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<ApiResponseDTO<String>> addNewRelease(NewReleaseDTO newReleaseDTO) {
        try {
            Set<Artist> artists = new HashSet<>();
            for (String artistId : newReleaseDTO.artistIds()) {
                Artist artist = artistRepository.findById(artistId).orElse(null);
                if (artist == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The artist with id as " + artistId + " does not exist", null));
                }
                artists.add(artist);
            }

            Set<Artist> featureArtists = new HashSet<>();
            for (String artistId : newReleaseDTO.featureIds()) {
                Artist artist = artistRepository.findById(artistId).orElse(null);
                if (artist == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The artist with id as " + artistId + " does not exist", null));
                }
                featureArtists.add(artist);
            }

            Releases release = Releases.builder()
                    .title(newReleaseDTO.title())
                    .releaseTimestamp(newReleaseDTO.releaseTimestamp())
                    .coverUrl(newReleaseDTO.coverUrl())
                    .releaseType(newReleaseDTO.releaseType())
                    .artists(artists)
                    .features(featureArtists)
                    .build();

            Releases savedRelease = releaseRepository.save(release);

            for (Artist artist : artists) {
                artist.getArtistReleases().add(savedRelease);
            }

            for (Artist artist : featureArtists) {
                artist.getFeatureReleases().add(savedRelease);
            }

            artistRepository.saveAll(artists);
            artistRepository.saveAll(featureArtists);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Saved release type successfully", savedRelease.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<String>> updateExistingRelease(UpdateReleaseDTO updateReleaseDTO, String releaseId) {
        try {
            Releases release = releaseRepository.findById(releaseId).orElse(null);
            if (release == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Release not found", null));
            }

            if (!updateReleaseDTO.title().isEmpty()) {
                release.setTitle(updateReleaseDTO.title());

                if (release.getReleaseType().equals(ReleaseType.SINGLE)) {
                    Set<Song> songs = release.getSongs();
                    songs.forEach(song -> {
                        song.setTitle(updateReleaseDTO.title());
                        song.setCoverUrl(updateReleaseDTO.coverUrl());
                    });
                }
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Release title cannot be empty", null));
            }

            release.setReleaseTimestamp(updateReleaseDTO.releaseTimestamp());
            release.setCoverUrl(updateReleaseDTO.coverUrl());

            Set<Artist> artists = new HashSet<>();
            for (String artistId : updateReleaseDTO.artistIds()) {
                Artist artist = artistRepository.findById(artistId).orElse(null);
                if (artist == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The artist with id as " + artistId + " does not exist", null));
                }
                artists.add(artist);
            }
            release.setArtists(artists);

            Set<Artist> featureArtists = new HashSet<>();
            for (String artistId : updateReleaseDTO.featureIds()) {
                Artist artist = artistRepository.findById(artistId).orElse(null);
                if (artist == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The artist with id as " + artistId + " does not exist", null));
                }
                featureArtists.add(artist);
            }
            release.setFeatures(featureArtists);

            Releases updatedRelease = releaseRepository.save(release);
            for (Artist artist : artists) {
                artist.getArtistReleases().add(updatedRelease);
            }

            for (Artist artist : featureArtists) {
                artist.getFeatureReleases().add(updatedRelease);
            }
            artistRepository.saveAll(artists);
            artistRepository.saveAll(featureArtists);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Saved release type successfully", updatedRelease.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingRelease(String releaseId) {
        try {
            if (!releaseRepository.existsById(releaseId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Release not found", null));
            }

            releaseRepository.deleteById(releaseId);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully deleted release", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Set<ReleasesPreviewDTO>>> getAllReleasesByArtistId(String artistId, ReleaseType releaseType) {
        try {
            if (artistId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artist not given in the payload", null));
            }

            Set<Releases> releases = releaseRepository.findArtistReleasesByArtistId(artistId, releaseType);
            Set<ReleasesPreviewDTO> releasesPreviewDTOS = new HashSet<>();
            releases.forEach(release -> releasesPreviewDTOS.add(objectMapper.convertValue(release, ReleasesPreviewDTO.class)));

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved releases", releasesPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }
}
