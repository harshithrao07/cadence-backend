package com.project.cadence.controller;

import com.project.cadence.dto.s3.FileUploadResult;
import com.project.cadence.dto.s3.SaveFileDTO;
import com.project.cadence.service.AwsService;
import com.project.cadence.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/files")
public class AwsController {
    private final AwsService awsService;
    private final JwtService jwtService;

    @PostMapping("/presigned-url")
    public ResponseEntity<Object> getPresignedUrl(HttpServletRequest request, @Valid @RequestBody SaveFileDTO saveFileDTO) {
        if (!jwtService.checkIfAdminFromHttpRequest(request)) {
            return new ResponseEntity<>("You are not authorized to perform this operation", HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(awsService.getPresignedUrl(saveFileDTO.category(), saveFileDTO.subCategory(), saveFileDTO.name(), saveFileDTO.extension()), HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResult>> save(HttpServletRequest request, @RequestPart("file") List<Part> parts) {
        if (!jwtService.checkIfAdminFromHttpRequest(request)) {
            return new ResponseEntity<>(null, HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(awsService.save(parts), HttpStatus.OK);
    }
}
