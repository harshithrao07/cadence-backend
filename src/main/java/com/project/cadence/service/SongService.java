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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.*;

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

    public ResponseEntity<ApiResponseDTO<List<AddSongResponseDTO>>> addNewSongs(NewSongsDTO newSongsDTO) {
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

                    Set<Artist> songArtists = new HashSet<>();
                    if (eachNewSongDTO.artistIds() != null) {
                        for (String artistId : eachNewSongDTO.artistIds()) {
                            Artist artist = artistRepository.findById(artistId).orElse(null);
                            if (artist == null) {
                                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The artist with id as " + artistId + " does not exist", null));
                            }
                            songArtists.add(artist);
                        }
                    }

                    boolean isSubset = new HashSet<>(songArtists).containsAll(record.get().getArtists());
                    if (!isSubset) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Not all artists of the record is there in the song", null));
                    }

                    Song song = Song.builder()
                            .title(eachNewSongDTO.title())
                            .totalDuration(eachNewSongDTO.totalDuration())
                            .coverUrl(record.get().getCoverUrl())
                            .record(record.get())
                            .genres(genres)
                            .order(eachNewSongDTO.order())
                            .coverUrl(eachNewSongDTO.coverUrl())
                            .build();

                    Song savedSong = songRepository.save(song);

                    for (Artist artist : songArtists) {
                        artist.getArtistSongs().add(savedSong);
                    }
                    artistRepository.saveAll(songArtists);

                    record.get().getSongs().add(savedSong);
                    recordRepository.save(record.get());

                    AddSongResponseDTO addSongResponseDTO = new AddSongResponseDTO(song.getId(), song.getTitle());
                    addSongResponseDTOList.add(addSongResponseDTO);
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

    public ResponseEntity<ApiResponseDTO<String>> updateExistingSong(UpdateSongDTO updateSongDTO, String songId) {
        try {
            Song song = songRepository.findById(songId).orElse(null);
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

            Set<Artist> songArtists = new HashSet<>();
            if (updateSongDTO.artistIds().isPresent()) {

                Set<Artist> newArtists = new HashSet<>();

                for (String artistId : updateSongDTO.artistIds().get()) {
                    Artist artist = artistRepository.findById(artistId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Artist not found: " + artistId)
                            );
                    newArtists.add(artist);
                }

                // ✅ Correct validation
                if (!record.getArtists().containsAll(newArtists)) {
                    return ResponseEntity.badRequest().body(
                            new ApiResponseDTO<>(false,
                                    "All song artists must belong to the record",
                                    null)
                    );
                }

                // ✅ DETACH safely (defensive copy)
                for (Artist oldArtist : new HashSet<>(song.getCreatedBy())) {
                    oldArtist.getArtistSongs().remove(song);
                }

                // ✅ ATTACH on OWNING SIDE
                for (Artist newArtist : newArtists) {
                    newArtist.getArtistSongs().add(song);
                }

                // ✅ Sync inverse side (memory only)
                song.setCreatedBy(newArtists);
            }


            if (updateSongDTO.coverUrl().isPresent()) {
                song.setCoverUrl(updateSongDTO.coverUrl().get());
            }

            songRepository.save(song);

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

            Set<Artist> songArtists = song.getCreatedBy();
            songArtists.forEach(songArtist -> songArtist.getArtistSongs().remove(song));

            Set<User> likedByUsers = song.getLikedBy();
            likedByUsers.forEach(likedByUser -> likedByUser.getLikedSongs().remove(song));

            Set<Playlist> playlistsAddedTo = song.getPlaylists();
            playlistsAddedTo.forEach(playlist -> playlist.getSongs().remove(song));

            Record record = recordRepository.findById(song.getRecord().getId()).orElse(null);
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
                Set<Artist> combinedArtists = new LinkedHashSet<>(artists);

                Set<TrackArtistInfoDTO> trackArtistInfoDTOS = new LinkedHashSet<>(); // LinkedHashSet maintains insertion order
                combinedArtists.forEach(artist -> trackArtistInfoDTOS.add(objectMapper.convertValue(artist, TrackArtistInfoDTO.class)));

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

            Set<TrackArtistInfoDTO> trackArtistInfoDTOS = new LinkedHashSet<>();
            Set<Artist> artists = song.getCreatedBy();

            Set<Artist> combinedArtists = new LinkedHashSet<>(artists);

            combinedArtists.forEach(artist -> trackArtistInfoDTOS.add(objectMapper.convertValue(artist, TrackArtistInfoDTO.class)));

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

        StreamingResponseBody stream = outputStream -> {
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
        return stream;
    }

    public ResponseEntity<ApiResponseDTO<List<SongInRecordDTO>>> getSongsByRecord(String recordId) {
        try {
            List<Song> songs = songRepository.findByRecordIdWithArtistsAndGenres(recordId);
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
