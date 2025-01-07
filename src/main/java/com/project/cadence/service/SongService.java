package com.project.cadence.service;

import com.amazonaws.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.TrackArtistInfoDTO;
import com.project.cadence.dto.record.TrackRecordInfoDTO;
import com.project.cadence.dto.song.EachNewSongDTO;
import com.project.cadence.dto.song.NewSongsDTO;
import com.project.cadence.dto.song.TrackPreviewDTO;
import com.project.cadence.dto.song.UpdateSongDTO;
import com.project.cadence.model.*;
import com.project.cadence.model.Record;
import com.project.cadence.repository.ArtistRepository;
import com.project.cadence.repository.GenreRepository;
import com.project.cadence.repository.RecordRepository;
import com.project.cadence.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final RecordRepository recordRepository;
    private final GenreRepository genreRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final RecordService recordService;
    private final AwsService awsService;
    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<ApiResponseDTO<String>> addNewSongs(NewSongsDTO newSongsDTO) {
        try {
            String recordId = newSongsDTO.recordId();
            Optional<Record> record = recordRepository.findById(recordId);
            if (record.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record with the given ID does not exist", null));
            }

            Set<EachNewSongDTO> eachNewSongDTOS = newSongsDTO.songs();

            if (record.get().getRecordType().equals(RecordType.SINGLE)) {
                if (eachNewSongDTOS.size() > 1 || !record.get().getSongs().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Given record is a SINGLE, cannot have more than one song", null));
                }
            }

            if (!eachNewSongDTOS.isEmpty()) {
                for (EachNewSongDTO eachNewSongDTO : eachNewSongDTOS) {
                    if (record.get().getRecordType().equals(RecordType.SINGLE) && !record.get().getTitle().equals(eachNewSongDTO.title())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Given record is a SINGLE, song title must match the record title", null));
                    }

                    Set<Genre> genres = new HashSet<>();
                    for (String genreId : eachNewSongDTO.genreIds()) {
                        Optional<Genre> genre = genreRepository.findById(genreId);
                        if (genre.isEmpty()) {
                            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The genre with id as " + genreId + " does not exist", null));
                        }
                        genres.add(genre.get());
                    }

                    Set<Artist> featureArtists = new HashSet<>();
                    if (eachNewSongDTO.featureIds() != null) {
                        for (String artistId : eachNewSongDTO.featureIds()) {
                            Artist artist = artistRepository.findById(artistId).orElse(null);
                            if (artist == null) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The feature artist with id as " + artistId + " does not exist", null));
                            }

                            if (record.get().getArtists().contains(artist)) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, artist.getName() + " is the main artist and cannot be a feature", null));
                            }
                            featureArtists.add(artist);
                        }
                    }

                    if (!awsService.findByName(eachNewSongDTO.songUrl())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, eachNewSongDTO.title() + " track has not been uploaded to S3 yet", null));
                    }

                    Song song = Song.builder()
                            .title(eachNewSongDTO.title())
                            .songUrl(eachNewSongDTO.songUrl())
                            .totalDuration(eachNewSongDTO.totalDuration())
                            .coverUrl(record.get().getCoverUrl())
                            .recordId(record.get().getId())
                            .genres(genres)
                            .build();

                    Song savedSong = songRepository.save(song);

                    Set<Artist> artists = record.get().getArtists();
                    for (Artist artist : artists) {
                        artist.getArtistSongs().add(savedSong);
                    }
                    artistRepository.saveAll(artists);

                    for (Artist artist : featureArtists) {
                        artist.getFeatureSongs().add(savedSong);
                    }
                    artistRepository.saveAll(featureArtists);

                    record.get().getSongs().add(savedSong);
                    recordRepository.save(record.get());
                }
                return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Songs added successfully for " + record.get().getTitle(), recordId));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Songs cannot be empty", null));
            }
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<String>> updateExistingSong(UpdateSongDTO updateSongDTO, String songId) {
        try {
            Song song = songRepository.findById(songId).orElse(null);
            if (song == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song with the given ID does not exist", null));
            }

            Record record = recordRepository.findById(song.getRecordId()).orElse(null);
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song is not tagged to any record", null));
            }

            if (updateSongDTO.title().isPresent()) {
                song.setTitle(updateSongDTO.title().get());
                if (record.getRecordType().equals(RecordType.SINGLE)) {
                    record.setTitle(updateSongDTO.title().get());
                }
            }

            if (updateSongDTO.songUrl().isPresent()) {
                if (!updateSongDTO.songUrl().get().equals(song.getSongUrl())) {
                    if (awsService.findByName(song.getSongUrl())) {
                        awsService.deleteObject(song.getSongUrl());
                    }

                    if (!awsService.findByName(updateSongDTO.songUrl().get())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, song.getTitle() + " track has not been uploaded to S3 yet", null));
                    } else {
                        song.setSongUrl(updateSongDTO.songUrl().get());
                    }
                }
            }

            if (updateSongDTO.totalDuration().isPresent()) {
                song.setTotalDuration(updateSongDTO.totalDuration().get());
            }

            if (updateSongDTO.genreIds().isPresent()) {
                if (updateSongDTO.genreIds().get().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Genre Id's cannot be empty", null));
                }

                Set<Genre> genres = new HashSet<>();
                for (String genreId : updateSongDTO.genreIds().get()) {
                    Optional<Genre> genre = genreRepository.findById(genreId);
                    if (genre.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The genre with id as " + genreId + " does not exist", null));
                    }
                    genres.add(genre.get());
                }
                song.setGenres(genres);
            }

            Set<Artist> featureArtists = new HashSet<>();
            if (updateSongDTO.featureIds().isPresent()) {
                for (String artistId : updateSongDTO.featureIds().get()) {
                    Optional<Artist> artist = artistRepository.findById(artistId);
                    if (artist.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The feature artist with id as " + artistId + " does not exist", null));
                    }

                    if (record.getArtists().contains(artist.get())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, artist.get().getName() + " is the main artist and cannot be a feature", null));
                    }
                    featureArtists.add(artist.get());
                }
                Set<Artist> oldFeatureArtists = song.getFeatures();
                oldFeatureArtists.forEach(oldFeatureArtist -> oldFeatureArtist.getFeatureSongs().remove(song));
                artistRepository.saveAll(oldFeatureArtists);
                song.setFeatures(featureArtists);
            }

            Song updatedSong = songRepository.save(song);
            featureArtists.forEach(featureArtist -> featureArtist.getFeatureSongs().add(updatedSong));
            artistRepository.saveAll(featureArtists);

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully updated the song", songId));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingSong(String songId) {
        try {
            Song song = songRepository.findById(songId).orElse(null);
            if (song == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song not found", null));
            }

            Set<Artist> artists = song.getCreatedBy();
            artists.forEach(artist -> artist.getArtistSongs().remove(song));

            Set<Artist> featureArtists = song.getFeatures();
            featureArtists.forEach(featureArtist -> featureArtist.getFeatureSongs().remove(song));

            Set<User> likedByUsers = song.getLikedBy();
            likedByUsers.forEach(likedByUser -> likedByUser.getLikedSongs().remove(song));

            Set<Playlist> playlistsAddedTo = song.getPlaylists();
            playlistsAddedTo.forEach(playlist -> playlist.getSongs().remove(song));

            Record record = recordRepository.findById(song.getRecordId()).orElse(null);
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song is not tagged to any record", null));
            }

            if (awsService.findByName(song.getSongUrl())) {
                awsService.deleteObject(song.getSongUrl());
            }

            if (record.getRecordType().equals(RecordType.SINGLE)) {
                recordService.deleteExistingRecord(record.getId());
            }

            songRepository.deleteById(songId);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully deleted the song", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Set<TrackPreviewDTO>>> getAllSongs(String artistId, String recordId) {
        try {
            Set<TrackPreviewDTO> trackPreviewDTOS = new HashSet<>();
            Set<Song> songs = songRepository.getAllSongs(artistId, recordId);

            Record record = null;
            if (recordId != null) {
                record = recordRepository.findById(recordId).orElse(null);
            }

            String eachRecordId = "";
            for (Song song : songs) {
                Set<Artist> artists = song.getCreatedBy();
                Set<Artist> features = song.getFeatures();
                Set<Artist> combinedArtists = new LinkedHashSet<>();
                combinedArtists.addAll(artists);
                combinedArtists.addAll(features);

                Set<TrackArtistInfoDTO> trackArtistInfoDTOS = new LinkedHashSet<>(); // LinkedHashSet maintains insertion order
                combinedArtists.forEach(artist -> trackArtistInfoDTOS.add(objectMapper.convertValue(artist, TrackArtistInfoDTO.class)));

                if (record == null || !eachRecordId.equals(song.getRecordId())) {
                    eachRecordId = song.getRecordId();
                    record = recordRepository.findById(eachRecordId).orElse(null);
                    if (record == null) {
                        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record not found", null));
                    }
                }

                TrackRecordInfoDTO trackRecordInfoDTO = objectMapper.convertValue(record, TrackRecordInfoDTO.class);

                TrackPreviewDTO trackPreviewDTO = objectMapper.convertValue(song, TrackPreviewDTO.class);
                TrackPreviewDTO updatedTrackPreviewDTO = new TrackPreviewDTO(
                        trackPreviewDTO.id(),
                        trackPreviewDTO.title(),
                        trackPreviewDTO.totalDuration(),
                        trackPreviewDTO.coverUrl(),
                        trackArtistInfoDTOS,
                        trackRecordInfoDTO
                );
                trackPreviewDTOS.add(updatedTrackPreviewDTO);
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Retrieved all songs for the particular record", trackPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<TrackPreviewDTO>> getSongById(String songId) {
        try {
            Song song = songRepository.findById(songId).orElse(null);
            if (song == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song not found", null));
            }

            Set<TrackArtistInfoDTO> trackArtistInfoDTOS = new LinkedHashSet<>();
            Set<Artist> artists = song.getCreatedBy();
            Set<Artist> featureArtists = song.getFeatures();

            Set<Artist> combinedArtists = new LinkedHashSet<>();
            combinedArtists.addAll(artists);
            combinedArtists.addAll(featureArtists);

            combinedArtists.forEach(artist -> trackArtistInfoDTOS.add(objectMapper.convertValue(artist, TrackArtistInfoDTO.class)));

            Record record = recordRepository.findById(song.getRecordId()).orElse(null);
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record not found", null));
            }

            TrackRecordInfoDTO trackRecordInfoDTO = objectMapper.convertValue(record, TrackRecordInfoDTO.class);
            TrackPreviewDTO trackPreviewDTO = objectMapper.convertValue(song, TrackPreviewDTO.class);
            TrackPreviewDTO updatedTrackPreviewDTO = new TrackPreviewDTO(
                    trackPreviewDTO.id(),
                    trackPreviewDTO.title(),
                    trackPreviewDTO.totalDuration(),
                    trackPreviewDTO.coverUrl(),
                    trackArtistInfoDTOS,
                    trackRecordInfoDTO
            );

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved the song", updatedTrackPreviewDTO));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

//    public void streamSongById(String songId) {
//        try {
//            String filename = "music/"
//        } catch (Exception e) {
//            log.error("An exception has occurred {}", e.getMessage(), e);
//        }
//    }
}
