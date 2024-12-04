package com.project.musicplayer.controller;

import com.project.musicplayer.dto.s3.SaveFileDTO;
import com.project.musicplayer.service.AwsService;
import com.project.musicplayer.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/files")
public class AwsController {
    private final AwsService awsService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<Object> saveFile(HttpServletRequest request, @RequestBody @NotNull SaveFileDTO saveFileDTO) {
        if (!jwtService.checkIfAdminFromHttpRequest(request)) {
            return new ResponseEntity<>("You are not authorized to perform this operation", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(awsService.save(saveFileDTO.category(), saveFileDTO.name(), saveFileDTO.extension()), HttpStatus.OK);
    }

}
