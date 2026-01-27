package com.project.cadence.service;

import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.genre.GenrePreviewDTO;
import com.project.cadence.dto.song.*;
import com.project.cadence.model.*;
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
    private final SongRepository songRepository;
    private final AwsService awsService;

    public ResponseEntity<ApiResponseDTO<List<EachSongDTO>>> getAllSongsByRecordId(String recordId) {
        try {
            if (!recordRepository.existsById(recordId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiResponseDTO<>(false, "Record not found", null));
            }

            List<Song> songs = recordRepository.getAllSongsByRecordId(recordId);
            List<EachSongDTO> eachSongDTOS = songs.stream().map(
                    song -> new EachSongDTO(
                            song.getId(),
                            song.getTitle(),
                            song.getTotalDuration(),
                            song.getCreatedBy().stream().map(
                                    artist -> new ArtistPreviewDTO(
                                            artist.getId(),
                                            artist.getName(),
                                            artist.getProfileUrl()
                                    )
                            ).toList(),
                            song.getGenres().stream().map(
                                    genre -> new GenrePreviewDTO(
                                            genre.getId(),
                                            genre.getType()
                                    )
                            ).toList(),
                            song.getRecord().getId()
                    )
            ).toList();

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Retrieved all songs for the given record", eachSongDTOS));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<ApiResponseDTO<EachSongDTO>> getSongById(String songId) {
        try {
            Song song = songRepository.findById(songId).orElse(null);
            if (song == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiResponseDTO<>(false, "Song not found", null));
            }

            EachSongDTO eachSongDTO = new EachSongDTO(
                    song.getId(),
                    song.getTitle(),
                    song.getTotalDuration(),
                    song.getCreatedBy().stream().map(
                            artist -> new ArtistPreviewDTO(
                                    artist.getId(),
                                    artist.getName(),
                                    artist.getProfileUrl()
                            )
                    ).toList(),
                    song.getGenres().stream().map(
                            genre -> new GenrePreviewDTO(
                                    genre.getId(),
                                    genre.getType()
                            )
                    ).toList(),
                    song.getRecord().getId()
            );

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved the song", eachSongDTO));
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
}
