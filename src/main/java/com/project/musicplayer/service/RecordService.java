package com.project.musicplayer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.record.NewRecordDTO;
import com.project.musicplayer.dto.record.RecordPreviewDTO;
import com.project.musicplayer.dto.record.UpdateRecordDTO;
import com.project.musicplayer.model.*;
import com.project.musicplayer.model.Record;
import com.project.musicplayer.repository.ArtistRepository;
import com.project.musicplayer.repository.RecordRepository;
import com.project.musicplayer.repository.SongRepository;
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
public class RecordService {
    private final RecordRepository recordRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    ObjectMapper objectMapper = new ObjectMapper();

    public ResponseEntity<ApiResponseDTO<String>> addNewRecord(NewRecordDTO newRecordDTO) {
        try {
            if (newRecordDTO.artistIds().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artists cannot be empty", null));
            }

            Set<Artist> artists = new HashSet<>();
            for (String artistId : newRecordDTO.artistIds()) {
                Artist artist = artistRepository.findById(artistId).orElse(null);
                if (artist == null) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The artist with id as " + artistId + " does not exist", null));
                }
                artists.add(artist);
            }

            Record record = Record.builder()
                    .title(newRecordDTO.title())
                    .releaseTimestamp(newRecordDTO.releaseTimestamp().orElse(System.currentTimeMillis()))
                    .coverUrl(newRecordDTO.coverUrl().orElse(""))
                    .recordType(newRecordDTO.recordType())
                    .artists(artists)
                    .build();

            Record savedRecord = recordRepository.save(record);

            for (Artist artist : artists) {
                artist.getArtistRecords().add(savedRecord);
            }

            artistRepository.saveAll(artists);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Added a new record successfully", savedRecord.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<String>> updateExistingRecord(UpdateRecordDTO updateRecordDTO, String recordId) {
        try {
            Record record = recordRepository.findById(recordId).orElse(null);
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record not found", null));
            }

            if (updateRecordDTO.title().isPresent()) {
                record.setTitle(updateRecordDTO.title().get());
                Set<Song> songs = record.getSongs();

                if (record.getRecordType().equals(RecordType.SINGLE)) {
                    songs.forEach(song -> {
                        song.setTitle(updateRecordDTO.title().get());
                    });
                }

                if (updateRecordDTO.coverUrl().isPresent()) {
                    record.setCoverUrl(updateRecordDTO.coverUrl().get());
                    songs.forEach(song -> {
                        song.setCoverUrl(updateRecordDTO.coverUrl().get());
                    });
                }

                songRepository.saveAll(songs);
            }

            if (updateRecordDTO.releaseTimestamp().isPresent()) {
                record.setReleaseTimestamp(updateRecordDTO.releaseTimestamp().get());
            }

            Set<Artist> artists = new HashSet<>();
            if (updateRecordDTO.artistIds().isPresent()) {
                if (updateRecordDTO.artistIds().get().isEmpty()) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artists cannot be empty", null));
                }

                for (String artistId : updateRecordDTO.artistIds().get()) {
                    Artist artist = artistRepository.findById(artistId).orElse(null);
                    if (artist == null) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The artist with id as " + artistId + " does not exist", null));
                    }
                    artists.add(artist);
                }
                record.setArtists(artists);
            }

            Record updatedRecord = recordRepository.save(record);
            for (Artist artist : artists) {
                artist.getArtistRecords().add(updatedRecord);
            }

            artistRepository.saveAll(artists);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Updated the record successfully", updatedRecord.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingRecord(String recordId) {
        try {
            Record record = recordRepository.findById(recordId).orElse(null);
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record not found", null));
            }

            Set<Artist> artists = record.getArtists();
            artists.forEach(artist -> {
                artist.getArtistRecords().remove(record);
                artistRepository.save(artist);
            });

            recordRepository.deleteById(recordId);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully deleted the record", null));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<Set<RecordPreviewDTO>>> getAllRecordsByArtistId(String artistId, RecordType recordType) {
        try {
            if (artistId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artist not given in the payload", null));
            }

            String recordTypeString = (recordType != null) ? recordType.toString() : null;
            Set<Record> records = recordRepository.findArtistRecordsByArtistId(artistId, recordTypeString);
            Set<RecordPreviewDTO> recordPreviewDTOS = new HashSet<>();
            records.forEach(record -> recordPreviewDTOS.add(objectMapper.convertValue(record, RecordPreviewDTO.class)));

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved records", recordPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<RecordPreviewDTO>> getRecordById(String recordId) {
        try {
            Record record = recordRepository.findById(recordId).orElse(null);
            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Record not found", null));
            }

            RecordPreviewDTO recordPreviewDTO = objectMapper.convertValue(record, RecordPreviewDTO.class);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved record " + recordId, recordPreviewDTO));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }
}
