package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.genre.GenrePreviewDTO;
import com.project.cadence.dto.genre.NewGenreDTO;
import com.project.cadence.service.GenreService;
import com.project.cadence.service.JwtService;
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

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewSong(HttpServletRequest request, @Validated @RequestBody NewGenreDTO newGenreDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return genreService.addNewGenre(newGenreDTO.type());
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<List<GenrePreviewDTO>>> getAllGenres(@RequestParam(required = false) String key) {
        return genreService.getAllGenres(key);
    }
}
