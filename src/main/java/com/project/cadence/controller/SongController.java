package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.song.*;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.SongService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;
import java.util.Set;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/song")
public class SongController {
    private final JwtService jwtService;
    private final SongService songService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<List<AddSongResponseDTO>>> addNewSongs(HttpServletRequest request, @Validated @RequestBody NewSongsDTO newSongsDTO, @RequestParam(required = false, defaultValue = "false") Boolean editMode) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return songService.addNewSongs(newSongsDTO, editMode);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @PutMapping("/update")
    public ResponseEntity<ApiResponseDTO<String>> updateExistingSongs(HttpServletRequest request, @Validated @RequestBody List<UpdateSongDTO> updateSongDTOs) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return songService.updateExistingSong(updateSongDTOs);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @DeleteMapping("/delete/{songId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingSong(HttpServletRequest request, @PathVariable("songId") String songId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return songService.deleteExistingSong(songId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<Set<TrackPreviewDTO>>> getAllSongsByRecordId(
            @RequestParam String artistId,
            @RequestParam(required = false) String recordId
    ) {
        return songService.getAllSongs(artistId, recordId);
    }

    @GetMapping("/{songId}")
    public ResponseEntity<ApiResponseDTO<TrackPreviewDTO>> getSongById(
            @PathVariable("songId") String songId
    ) {
        return songService.getSongById(songId);
    }

    @GetMapping("/stream/{songId}")
    public ResponseEntity<StreamingResponseBody> streamSongById(@PathVariable("songId") String songId) {
        return songService.streamSongById(songId);
    }

    @GetMapping("/songsByRecord/{recordId}")
    public ResponseEntity<ApiResponseDTO<List<SongInRecordDTO>>> getSongsByRecord(@PathVariable("recordId") String recordId) {
        return songService.getSongsByRecord(recordId);
    }
}
