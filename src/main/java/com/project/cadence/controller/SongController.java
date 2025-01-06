package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.song.NewSongsDTO;
import com.project.cadence.dto.song.TrackPreviewDTO;
import com.project.cadence.dto.song.UpdateSongDTO;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.SongService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/song")
public class SongController {
    private final JwtService jwtService;
    private final SongService songService;

    @PostMapping("/admin/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewSongs(HttpServletRequest request, @Validated @RequestBody NewSongsDTO newSongsDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return songService.addNewSongs(newSongsDTO);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @PutMapping("/admin/update/{songId}")
    public ResponseEntity<ApiResponseDTO<String>> updateExistingSong(HttpServletRequest request, @Validated @RequestBody UpdateSongDTO updateSongDTO, @PathVariable("songId") String songId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return songService.updateExistingSong(updateSongDTO, songId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @DeleteMapping("/admin/delete/{songId}")
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
}
