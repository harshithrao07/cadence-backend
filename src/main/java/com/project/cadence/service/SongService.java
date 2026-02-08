package com.project.cadence.service;

import com.amazonaws.services.s3.model.S3Object;
import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.artist.ArtistPreviewDTO;
import com.project.cadence.dto.genre.GenrePreviewDTO;
import com.project.cadence.dto.record.RecordPreviewWithCoverImageDTO;
import com.project.cadence.dto.song.*;
import com.project.cadence.events.StreamSongEvent;
import com.project.cadence.model.*;
import com.project.cadence.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {
    private final RecordRepository recordRepository;
    private final SongRepository songRepository;
    private final AwsService awsService;
    private final UserRepository userRepository;
    private final ApplicationEventPublisher publisher;

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
                            new RecordPreviewWithCoverImageDTO(
                                    song.getRecord().getId(),
                                    song.getRecord().getTitle(),
                                    song.getRecord().getCoverUrl(),
                                    song.getRecord().getReleaseTimestamp()
                            )
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
                    new RecordPreviewWithCoverImageDTO(
                            song.getRecord().getId(),
                            song.getRecord().getTitle(),
                            song.getRecord().getCoverUrl(),
                            song.getRecord().getReleaseTimestamp()
                    )
            );

            return ResponseEntity.status(HttpStatus.OK).body(new ApiResponseDTO<>(true, "Successfully retrieved the song", eachSongDTO));
        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ApiResponseDTO<>(false, "An error occurred in the server", null));
        }
    }

    public ResponseEntity<StreamingResponseBody> streamSongById(
            String songId,
            String email,
            String rangeHeader
    ) {
        try {
            Song song = songRepository.findById(songId).orElse(null);
            if (song == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
            }

            User user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
            }

            publisher.publishEvent(new StreamSongEvent(user.getId(), songId));

            String songUrl = song.getSongUrl();
            String objectKey = awsService.extractKeyFromUrl(songUrl);

            /*
             * ============================
             * CASE 1: FILE EXISTS IN S3
             * ============================
             */
            if (objectKey != null && !objectKey.isEmpty() && awsService.findByName(objectKey)) {
                long fileSize = awsService.getFileSize(objectKey);

                long start = 0;
                long end = fileSize - 1;

                if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {

                    String rangeValue = rangeHeader.substring(6).trim();
                    String[] ranges = rangeValue.split("-");

                    // Start
                    if (!ranges[0].isEmpty()) {
                        start = Long.parseLong(ranges[0]);
                    }

                    // End (if provided)
                    if (ranges.length > 1 && !ranges[1].isEmpty()) {
                        end = Long.parseLong(ranges[1]);
                    } else {
                        end = fileSize - 1;
                    }

                    // Safety check
                    if (end >= fileSize) {
                        end = fileSize - 1;
                    }

                    if (start > end) {
                        start = 0;
                        end = fileSize - 1;
                    }
                }


                long contentLength = end - start + 1;

                S3Object s3Object = awsService.getObjectWithRange(objectKey, start, end);

                StreamingResponseBody stream = outputStream -> {
                    try (InputStream in = s3Object.getObjectContent()) {
                        byte[] buffer = new byte[8192];
                        int bytesRead;
                        while ((bytesRead = in.read(buffer)) != -1) {
                            outputStream.write(buffer, 0, bytesRead);
                        }
                    }
                };

                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.CONTENT_TYPE,
                        s3Object.getObjectMetadata().getContentType() != null
                                ? s3Object.getObjectMetadata().getContentType()
                                : "audio/mpeg");

                headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
                headers.set(HttpHeaders.CONTENT_RANGE,
                        "bytes " + start + "-" + end + "/" + fileSize);
                headers.setContentLength(contentLength);

                return new ResponseEntity<>(stream, headers, HttpStatus.PARTIAL_CONTENT);
            }

            /*
             * ============================
             * CASE 2: FALLBACK TO EXTERNAL URL
             * ============================
             */

            HttpURLConnection connection = (HttpURLConnection) new URL(songUrl).openConnection();
            connection.setRequestMethod("GET");

            if (rangeHeader != null) {
                connection.setRequestProperty("Range", rangeHeader);
            }

            connection.connect();

            int responseCode = connection.getResponseCode();
            long fileSize = connection.getContentLengthLong();

            long start = 0;
            long end = fileSize - 1;

            if (rangeHeader != null && rangeHeader.startsWith("bytes=")) {

                String rangeValue = rangeHeader.substring(6).trim();
                String[] ranges = rangeValue.split("-");

                // Start
                if (!ranges[0].isEmpty()) {
                    start = Long.parseLong(ranges[0]);
                }

                // End (if provided)
                if (ranges.length > 1 && !ranges[1].isEmpty()) {
                    end = Long.parseLong(ranges[1]);
                } else {
                    end = fileSize - 1;
                }

                // Safety check
                if (end >= fileSize) {
                    end = fileSize - 1;
                }

                if (start > end) {
                    start = 0;
                    end = fileSize - 1;
                }
            }


            long contentLength = end - start + 1;

            StreamingResponseBody stream = outputStream -> {
                try (InputStream in = connection.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = in.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_TYPE,
                    connection.getContentType() != null
                            ? connection.getContentType()
                            : "audio/mpeg");

            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");

            if (responseCode == 206) {
                headers.set(HttpHeaders.CONTENT_RANGE,
                        connection.getHeaderField("Content-Range"));
                headers.setContentLength(contentLength);
                return new ResponseEntity<>(stream, headers, HttpStatus.PARTIAL_CONTENT);
            }

            headers.setContentLength(fileSize);
            return new ResponseEntity<>(stream, headers, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Streaming error: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    public List<EachSongDTO> getRecordsForSearch(Pageable pageable, String key) {
        try {
            String searchKey = (key == null) ? "" : key.trim();
            Page<Song> songPage = songRepository
                    .findByTitleContainingIgnoreCase(
                            searchKey,
                            pageable
                    );

            return songPage.getContent()
                    .stream()
                    .map(song -> new EachSongDTO(
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
                                    new RecordPreviewWithCoverImageDTO(
                                            song.getRecord().getId(),
                                            song.getRecord().getTitle(),
                                            song.getRecord().getCoverUrl(),
                                            song.getRecord().getReleaseTimestamp()
                                    )
                            )
                    )
                    .toList();

        } catch (Exception e) {
            log.error("An exception has occurred {}", e.getMessage(), e);
            return List.of();
        }
    }

}
