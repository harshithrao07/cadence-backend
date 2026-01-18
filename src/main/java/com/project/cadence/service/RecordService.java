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
import jakarta.transaction.Transactional;
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

    @Transactional
    public ResponseEntity<ApiResponseDTO<String>> addNewRecord(NewRecordDTO newRecordDTO) {
        try {
            if (newRecordDTO.artistIds().isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artists cannot be empty", null));
            }

            List<Artist> artists = new ArrayList<>();
            for (String artistId : newRecordDTO.artistIds()) {
                Artist artist = artistRepository.findById(artistId)
                        .orElseThrow(() ->
                                new IllegalArgumentException("Artist not found: " + artistId)
                        );
                artists.add(artist);
            }

            Record record = Record.builder()
                    .title(newRecordDTO.title())
                    .releaseTimestamp(newRecordDTO.releaseTimestamp())
                    .recordType(newRecordDTO.recordType())
                    .build();

            record.getArtists().clear();
            record.getArtists().addAll(artists);

            Record savedRecord = recordRepository.save(record);

            return ResponseEntity.status(HttpStatus.CREATED).body(new ApiResponseDTO<>(true, "Added a new record successfully", savedRecord.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    @Transactional
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

            if (updateRecordDTO.artistIds().isPresent()) {
                List<Artist> artists = new ArrayList<>();

                for (String artistId : updateRecordDTO.artistIds().get()) {
                    Artist artist = artistRepository.findById(artistId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Artist not found: " + artistId)
                            );
                    artists.add(artist);
                }

                record.getArtists().clear();
                record.getArtists().addAll(artists);
            }

            recordRepository.save(record);
            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Updated the record successfully", record.getId()));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    @Transactional
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingRecord(String recordId) {

        Record record = recordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Record not found"));

        record.getSongs().forEach(song -> {
            if (song.getSongUrl() != null) {
                awsService.deleteObject(awsService.extractKeyFromUrl(song.getSongUrl()));
            }
        });

        if (record.getCoverUrl() != null) {
            awsService.deleteObject(awsService.extractKeyFromUrl(record.getCoverUrl()));
        }

        recordRepository.delete(record);

        return ResponseEntity.ok(
                new ApiResponseDTO<>(true, "Successfully deleted the record", null)
        );
    }

    public ResponseEntity<ApiResponseDTO<List<RecordPreviewDTO>>> getAllRecordsByArtistId(String artistId, RecordType recordType) {
        try {
            if (artistId == null) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Artist not given in the payload", null));
            }

            Set<Record> records = recordRepository.findArtistRecordsByArtistId(artistId, recordType);

            List<RecordPreviewDTO> recordPreviewDTOS = new ArrayList<>();
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
