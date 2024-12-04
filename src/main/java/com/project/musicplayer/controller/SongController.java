package com.project.musicplayer.controller;

import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.song.NewSongDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/song")
public class SongController {

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<Set<String>> addNewSongs(HttpServletRequest request, NewSongDTO)

}
