package com.project.cadence.service;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
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
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecordService {
    private final RecordRepository recordRepository;
    private final ArtistRepository artistRepository;
    private final SongRepository songRepository;
    private final AwsService awsService;

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
                    .releaseTimestamp(newRecordDTO.releaseTimestamp())
                    .recordType(newRecordDTO.recordType())
                    .artists(artists)
                    .build();

            Record savedRecord = recordRepository.save(record);

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true, "Added a new record successfully", savedRecord.getId()));
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

            Set<Record> records = recordRepository.findArtistRecordsByArtistId(artistId, recordType);

            Set<RecordPreviewDTO> recordPreviewDTOS = new HashSet<>();
            records.forEach(record -> {
                List<ArtistPreviewDTO> artistPreviewDTOS = new ArrayList<>();
                for (Artist artist : record.getArtists()) {
                    artistPreviewDTOS.add(new ArtistPreviewDTO(
                            artist.getId(),
                            artist.getName(),
                            artist.getProfileUrl()
                    ));
                }

                recordPreviewDTOS.add(new RecordPreviewDTO(
                        record.getId(),
                        record.getTitle(),
                        record.getReleaseTimestamp(),
                        record.getCoverUrl(),
                        record.getRecordType(),
                        artistPreviewDTOS
                ));
            });

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved records", recordPreviewDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<RecordPreviewDTO>> getRecordById(String recordId) {
        try {
            Record record = recordRepository.findByIdWithArtists(recordId).orElse(null);

            if (record == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponseDTO<>(false, "Record not found", null));
            }

            List<ArtistPreviewDTO> artistPreviewDTOS = new ArrayList<>();
            for (Artist artist : record.getArtists()) {
                artistPreviewDTOS.add(new ArtistPreviewDTO(
                        artist.getId(),
                        artist.getName(),
                        artist.getProfileUrl()
                ));
            }

            RecordPreviewDTO recordPreviewDTO = new RecordPreviewDTO(
                    record.getId(),
                    record.getTitle(),
                    record.getReleaseTimestamp(),
                    record.getCoverUrl(),
                    record.getRecordType(),
                    artistPreviewDTOS
            );

            return ResponseEntity.ok(
                    new ApiResponseDTO<>(true, "Successfully retrieved record " + recordId, recordPreviewDTO)
            );

        } catch (Exception e) {
            log.error("An exception has occurred", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

}
