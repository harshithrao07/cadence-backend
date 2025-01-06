package com.project.cadence.controller;

import com.project.cadence.dto.s3.SaveFileDTO;
import com.project.cadence.service.AwsService;
import com.project.cadence.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/files")
public class AwsController {
    private final AwsService awsService;
    private final JwtService jwtService;

    @PostMapping
    public ResponseEntity<Object> saveFile(HttpServletRequest request, @Valid @RequestBody SaveFileDTO saveFileDTO) {
        if (!jwtService.checkIfAdminFromHttpRequest(request)) {
            return new ResponseEntity<>("You are not authorized to perform this operation", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(awsService.save(saveFileDTO.category(), saveFileDTO.subCategory(), saveFileDTO.name(), saveFileDTO.extension()), HttpStatus.OK);
    }

}
