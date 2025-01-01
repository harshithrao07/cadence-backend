package com.project.musicplayer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.artist.TrackArtistInfoDTO;
import com.project.musicplayer.dto.genre.NewGenreDTO;
import com.project.musicplayer.dto.record.TrackRecordInfoDTO;
import com.project.musicplayer.dto.song.EachNewSongDTO;
import com.project.musicplayer.dto.song.NewSongsDTO;
import com.project.musicplayer.dto.song.TrackPreviewDTO;
import com.project.musicplayer.dto.song.UpdateSongDTO;
import com.project.musicplayer.model.*;
import com.project.musicplayer.model.Record;
import com.project.musicplayer.repository.ArtistRepository;
import com.project.musicplayer.repository.GenreRepository;
import com.project.musicplayer.repository.RecordRepository;
import com.project.musicplayer.repository.SongRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.HashSet;
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
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<ApiResponseDTO<String>> addNewSongs(NewSongsDTO newSongsDTO) {
        try {
            String recordId = newSongsDTO.recordId();
            Optional<Record> record = recordRepository.findById(recordId);
            if (record.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record with the given ID does not exist", null));
            }

            Set<EachNewSongDTO> eachNewSongDTOS = newSongsDTO.songs();
            for (EachNewSongDTO eachNewSongDTO : eachNewSongDTOS) {
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
                        featureArtists.add(artist);
                    }
                }

                Song song = Song.builder()
                        .title(eachNewSongDTO.title())
                        .songUrl(eachNewSongDTO.songUrl())
                        .totalDuration(eachNewSongDTO.totalDuration())
                        .coverUrl(record.get().getCoverUrl())
                        .recordId(record.get().getId())
                        .createdBy(record.get().getArtists())
                        .features(featureArtists)
                        .genres(genres)
                        .build();

                Song savedSong = songRepository.save(song);

                for (Artist artist : featureArtists) {
                    artist.getArtistSongs().add(savedSong);
                }

                for (Artist artist : featureArtists) {
                    artist.getFeatureSongs().add(savedSong);
                }

                artistRepository.saveAll(featureArtists);

                genres.forEach(genre -> genre.getSongs().add(savedSong));
                genreRepository.saveAll(genres);

                record.get().getSongs().add(savedSong);
                recordRepository.save(record.get());
            }

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Songs added successfully for " + record.get().getTitle(), recordId));
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

            if (updateSongDTO.coverUrl().isPresent()) {
                song.setCoverUrl(updateSongDTO.coverUrl().get());
                if (record.getRecordType().equals(RecordType.SINGLE)) {
                    record.setCoverUrl(updateSongDTO.coverUrl().get());
                }
            }

            if (updateSongDTO.songUrl().isPresent()) {
                song.setSongUrl(updateSongDTO.songUrl().get());
            }

            if (updateSongDTO.totalDuration().isPresent()) {
                song.setTotalDuration(updateSongDTO.totalDuration().get());
            }

            if (updateSongDTO.genreIds().isPresent()) {
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

            if (updateSongDTO.features().isPresent()) {
                Set<Artist> artists = new HashSet<>();
                for (String artistId : updateSongDTO.features().get()) {
                    Optional<Artist> artist = artistRepository.findById(artistId);
                    if (artist.isEmpty()) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The feature artist with id as " + artistId + " does not exist", null));
                    }
                    artists.add(artist.get());
                }
                song.setFeatures(artists);
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
            if (!songRepository.existsById(songId)) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song not found", null));
            }

            songRepository.deleteById(songId);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully deleted the song", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Set<TrackPreviewDTO>>> getAllSongsByRecordId(String recordId) {
        try {
            Record record = recordRepository.findById(recordId).orElse(null);
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record not found", null));
            }

            Set<TrackPreviewDTO> trackPreviewDTOS = new HashSet<>();
            Set<Song> songs = record.getSongs();
            songs.forEach(song -> {
                Set<Artist> artists = song.getCreatedBy();
                Set<Artist> features = song.getFeatures();
                artists.addAll(features);

                Set<TrackArtistInfoDTO> trackArtistInfoDTOS = new HashSet<>();
                artists.forEach(artist -> trackArtistInfoDTOS.add(objectMapper.convertValue(artist, TrackArtistInfoDTO.class)));

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
            });

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

            Set<TrackArtistInfoDTO> trackArtistInfoDTOS = new HashSet<>();
            Set<Artist> artists = song.getCreatedBy();
            artists.forEach(artist -> trackArtistInfoDTOS.add(objectMapper.convertValue(artist, TrackArtistInfoDTO.class)));

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

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved the song", trackPreviewDTO));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }
}
