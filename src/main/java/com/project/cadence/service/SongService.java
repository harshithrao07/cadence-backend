package com.project.cadence.service;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.artist.TrackArtistInfoDTO;
import com.project.cadence.dto.genre.GenrePreviewDTO;
import com.project.cadence.dto.record.TrackRecordInfoDTO;
import com.project.cadence.dto.song.*;
import com.project.cadence.model.*;
import com.project.cadence.model.Record;
import com.project.cadence.repository.ArtistRepository;
import com.project.cadence.repository.GenreRepository;
import com.project.cadence.repository.RecordRepository;
import com.project.cadence.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.*;
import java.util.stream.Collectors;

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
    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Transactional
    public ResponseEntity<ApiResponseDTO<List<AddSongResponseDTO>>> addNewSongs(NewSongsDTO newSongsDTO, Boolean editMode) {
        try {
            String recordId = newSongsDTO.recordId();
            Optional<Record> record = recordRepository.findById(recordId);
            if (record.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record with the given ID does not exist", null));
            }

            List<EachNewSongDTO> eachNewSongDTOS = newSongsDTO.songs();

            if (record.get().getRecordType().equals(RecordType.SINGLE)) {
                if (eachNewSongDTOS.size() > 1 || !record.get().getSongs().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Given record is a SINGLE, cannot have more than one song", null));
                }
            }

            Set<String> recordArtistIds =
                    record.get().getArtists()
                            .stream()
                            .map(Artist::getId)
                            .collect(Collectors.toSet());

            List<Song> orderedSongs =
                    songRepository.findByRecordOrderByOrderAsc(record.get());

            List<AddSongResponseDTO> addSongResponseDTOList = new ArrayList<>();
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

                    List<Artist> songArtists = new ArrayList<>();
                    if (eachNewSongDTO.artistIds() != null) {
                        for (String artistId : eachNewSongDTO.artistIds()) {
                            Artist artist = artistRepository.findById(artistId)
                                    .orElseThrow(() ->
                                            new IllegalArgumentException("Artist not found: " + artistId)
                                    );
                            songArtists.add(artist);
                        }
                    }

                    Set<String> songArtistIds = songArtists.stream()
                            .map(Artist::getId)
                            .collect(Collectors.toSet());

                    if (!songArtistIds.containsAll(recordArtistIds)) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                                .body(new ApiResponseDTO<>(
                                        false,
                                        "Song must contain all artists of the record",
                                        null
                                ));
                    }


                    Song song = Song.builder()
                            .title(eachNewSongDTO.title())
                            .totalDuration(eachNewSongDTO.totalDuration())
                            .record(record.get())
                            .genres(genres)
                            .build();

                    song.getCreatedBy().clear();
                    song.getCreatedBy().addAll(songArtists);

                    if (editMode) {
                        // INSERT mode
                        int insertIndex = eachNewSongDTO.order();

                        if (insertIndex < 0) {
                            return ResponseEntity.badRequest()
                                    .body(new ApiResponseDTO<>(
                                            false,
                                            "Invalid song order position",
                                            null
                                    ));
                        }

                        orderedSongs.add(insertIndex, song);

                    } else {
                        // APPEND mode
                        orderedSongs.add(song);
                    }
                }

                for (int i = 0; i < orderedSongs.size(); i++) {
                    orderedSongs.get(i).setOrder(i);
                }

                songRepository.saveAll(orderedSongs);
                for (Song song : orderedSongs) {
                    if (song.getRecord().getId().equals(record.get().getId())) {
                        addSongResponseDTOList.add(
                                new AddSongResponseDTO(song.getId(), song.getTitle())
                        );
                    }
                }

                return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true, "Songs added successfully for " + record.get().getTitle(), addSongResponseDTOList));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Songs cannot be empty", null));
            }
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponseDTO<String>> updateExistingSong(List<UpdateSongDTO> updateSongDTOs) {
        try {
            for (UpdateSongDTO updateSongDTO : updateSongDTOs) {
                Song song = songRepository.findById(updateSongDTO.id()).orElse(null);
                if (song == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song with the given ID does not exist", null));
                }

                Record record = recordRepository.findById(song.getRecord().getId()).orElse(null);
                if (record == null) {
                    return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song is not tagged to any record", null));
                }

                if (updateSongDTO.title().isPresent()) {
                    song.setTitle(updateSongDTO.title().get());
                    if (record.getRecordType().equals(RecordType.SINGLE)) {
                        record.setTitle(updateSongDTO.title().get());
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

                Set<String> recordArtistIds =
                        record.getArtists()
                                .stream()
                                .map(Artist::getId)
                                .collect(Collectors.toSet());

                if (updateSongDTO.artistIds().isPresent()) {
                    List<Artist> newArtists = new ArrayList<>();

                    for (String artistId : updateSongDTO.artistIds().get()) {
                        Artist artist = artistRepository.findById(artistId)
                                .orElseThrow(() ->
                                        new IllegalArgumentException("Artist not found: " + artistId)
                                );
                        newArtists.add(artist);
                    }

                    Set<String> songArtistIds = newArtists.stream()
                            .map(Artist::getId)
                            .collect(Collectors.toSet());

                    if (!songArtistIds.containsAll(recordArtistIds)) {
                        return ResponseEntity.badRequest().body(
                                new ApiResponseDTO<>(
                                        false,
                                        "Song must contain all artists of the record",
                                        null
                                )
                        );
                    }

                    jdbcTemplate.update(
                            "DELETE FROM artist_created_songs WHERE song_id = ?",
                            song.getId()
                    );

                    song.getCreatedBy().clear();
                    song.getCreatedBy().addAll(newArtists);
                }

                song.setOrder(updateSongDTO.order());
                songRepository.save(song);
            }
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully updated the song", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingSong(String songId) {
        try {
            Song song = songRepository.findById(songId).orElse(null);
            if (song == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Song not found", null));
            }

            // Remove from users' liked songs
            song.getLikedBy()
                    .forEach(user -> user.getLikedSongs().remove(song));

            // Remove from playlists
            song.getPlaylists()
                    .forEach(playlist -> playlist.getSongs().remove(song));

            Record record = song.getRecord();
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Song is not tagged to any record", null));
            }

            // Delete audio from S3
            if (song.getSongUrl() != null) {
                String key = awsService.extractKeyFromUrl(song.getSongUrl());
                if (awsService.findByName(key)) {
                    awsService.deleteObject(key);
                }
            }

            // SINGLE record cleanup
            if (record.getRecordType().equals(RecordType.SINGLE)) {
                recordService.deleteExistingRecord(record.getId());
                return ResponseEntity.ok(
                        new ApiResponseDTO<>(true, "Successfully deleted the song", null)
                );
            }

            List<Song> orderedSongs =
                    songRepository.findByRecordOrderByOrderAsc(record);

            orderedSongs.removeIf(s -> s.getId().equals(song.getId()));

            for (int i = 0; i < orderedSongs.size(); i++) {
                orderedSongs.get(i).setOrder(i);
            }

            songRepository.saveAll(orderedSongs);

            songRepository.delete(song);

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Successfully deleted the song", null)
            );
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
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
                List<Artist> artists = song.getCreatedBy();

                List<TrackArtistInfoDTO> trackArtistInfoDTOS = artists.stream()
                        .map(artist -> objectMapper.convertValue(artist, TrackArtistInfoDTO.class))
                        .toList();

                if (record == null || !eachRecordId.equals(song.getRecord().getId())) {
                    eachRecordId = song.getRecord().getId();
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
                        songRepository.getTotalPlaysForSong(song.getId()),
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

            List<TrackArtistInfoDTO> trackArtistInfoDTOS =
                    song.getCreatedBy()
                            .stream()
                            .map(artist -> objectMapper.convertValue(artist, TrackArtistInfoDTO.class))
                            .toList();

            Record record = recordRepository.findById(song.getRecord().getId()).orElse(null);
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
                    songRepository.getTotalPlaysForSong(songId),
                    trackArtistInfoDTOS,
                    trackRecordInfoDTO
            );

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved the song", updatedTrackPreviewDTO));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<StreamingResponseBody> streamSongById(String songId) {
        try {
            Song song = songRepository.findById(songId).orElse(null);
            if (song == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            String songUrl = song.getSongUrl();
            String objectKey = awsService.extractKeyFromUrl(songUrl);
            if (!awsService.findByName(objectKey)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            S3Object s3Object = awsService.getObject(objectKey);
            StreamingResponseBody stream = getStreamingResponseBody(s3Object);
            return ResponseEntity.status(HttpStatus.OK).body(stream);
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    private static @NotNull StreamingResponseBody getStreamingResponseBody(@NotNull S3Object s3Object) {
        S3ObjectInputStream s3ObjectInputStream = s3Object.getObjectContent();

        return outputStream -> {
            try (s3ObjectInputStream) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = s3ObjectInputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
            } catch (Exception e) {
                log.error("An exception has occurred {}", e.getMessage(), e);
            }
        };
    }

    public ResponseEntity<ApiResponseDTO<List<SongInRecordDTO>>> getSongsByRecord(String recordId) {
        try {
            List<Song> songs = songRepository.findByRecordIdWithGenres(recordId);
            Optional<Record> record = recordRepository.findById(recordId);
            List<SongInRecordDTO> songDTOS = new ArrayList<>();
            for (Song song : songs) {
                List<ArtistPreviewDTO> artistPreviewDTOS =
                        song.getCreatedBy()
                                .stream()
                                .map(artist -> new ArtistPreviewDTO(
                                        artist.getId(),
                                        artist.getName(),
                                        artist.getProfileUrl()
                                ))
                                .toList();

                List<GenrePreviewDTO> genrePreviewDTOS =
                        song.getGenres()
                                .stream()
                                .map(genre -> new GenrePreviewDTO(
                                        genre.getId(),
                                        genre.getType()
                                ))
                                .toList();

                songDTOS.add(new SongInRecordDTO(
                        song.getId(),
                        song.getTitle(),
                        song.getTotalDuration(),
                        record.map(Record::getCoverUrl).orElse(null),
                        artistPreviewDTOS,
                        genrePreviewDTOS,
                        song.getOrder()
                ));
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved the songs", songDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }
}
