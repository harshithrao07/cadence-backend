package com.project.musicplayer.controller;

import com.project.musicplayer.dto.ApiResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/song")
public class SongController {

    @PostMapping(path = "/add")
    public ResponseEntity<ApiResponseDTO<String>> addSong(HttpServletRequest request, )

}
