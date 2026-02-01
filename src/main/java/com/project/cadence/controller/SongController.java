package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.song.*;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.SongService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/song")
public class SongController {
    private final SongService songService;
    private final JwtService jwtService;

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<List<EachSongDTO>>> getAllSongsByRecordId(
            @RequestParam String recordId
    ) {
        return songService.getAllSongsByRecordId(recordId);
    }

    @GetMapping("/{songId}")
    public ResponseEntity<ApiResponseDTO<EachSongDTO>> getSongById(
            @PathVariable("songId") String songId
    ) {
        return songService.getSongById(songId);
    }

    @GetMapping("/stream/{songId}")
    public ResponseEntity<StreamingResponseBody> streamSongById(HttpServletRequest request, @PathVariable("songId") String songId) {
        String email = jwtService.getEmailFromHttpRequest(request);
        return songService.streamSongById(songId, email);
    }
}
