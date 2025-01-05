package com.project.musicplayer.controller;

import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.genre.GenrePreviewDTO;
import com.project.musicplayer.dto.genre.NewGenreDTO;
import com.project.musicplayer.service.GenreService;
import com.project.musicplayer.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/genre")
public class GenreController {
    private final GenreService genreService;
    private final JwtService jwtService;

    @PostMapping("/admin/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewSong(HttpServletRequest request, @Validated @RequestBody NewGenreDTO newGenreDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return genreService.addNewGenre(newGenreDTO.type());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<List<GenrePreviewDTO>>> getAllGenres() {
        return genreService.getAllGenres();
    }
}
