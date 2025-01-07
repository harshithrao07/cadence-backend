package com.project.cadence.service;

import com.amazonaws.HttpMethod;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.record.NewRecordDTO;
import com.project.cadence.dto.record.RecordPreviewDTO;
import com.project.cadence.dto.record.UpdateRecordDTO;
import com.project.cadence.model.*;
import com.project.cadence.model.Record;
import com.project.cadence.repository.ArtistRepository;
import com.project.cadence.repository.RecordRepository;
import com.project.cadence.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
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
    private final AwsService awsService;
    private final RestClient restClient;
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

            if (newRecordDTO.coverUrl().isPresent()) {
                if (!awsService.findByName(newRecordDTO.coverUrl().get())) {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "The cover image for the record has not been uploaded to S3 yet", null));
                }
            }

            Record record = Record.builder()
                    .title(newRecordDTO.title())
                    .releaseTimestamp(newRecordDTO.releaseTimestamp().orElse(System.currentTimeMillis()))
                    .coverUrl(newRecordDTO.coverUrl().orElse("/"))
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
                    songs.forEach(song -> song.setTitle(updateRecordDTO.title().get()));
                }

                songRepository.saveAll(songs);
            }

            if (updateRecordDTO.coverUrl().isPresent()) {
                if (!updateRecordDTO.coverUrl().get().equals(record.getCoverUrl()) && !updateRecordDTO.coverUrl().get().isEmpty()) {
                    if (awsService.findByName(record.getCoverUrl())) {
                        awsService.deleteObject(record.getCoverUrl());
                    }

                    if (!awsService.findByName(updateRecordDTO.coverUrl().get())) {
                        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Updated " + record.getTitle() + " cover image has not been uploaded to S3 yet", null));
                    } else {
                        record.setCoverUrl(updateRecordDTO.coverUrl().get());
                        Set<Song> songs = record.getSongs();
                        songs.forEach(song -> song.setCoverUrl(updateRecordDTO.coverUrl().get()));
                        songRepository.saveAll(songs);
                    }
                }
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

            if (awsService.findByName(record.getCoverUrl())) {
                awsService.deleteObject(record.getCoverUrl());
            }

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
