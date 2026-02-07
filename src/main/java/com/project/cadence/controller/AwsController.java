package com.project.cadence.controller;

import com.project.cadence.dto.s3.FileUploadResult;
import com.project.cadence.dto.s3.MetadataDTO;
import com.project.cadence.service.AwsService;
import jakarta.servlet.http.Part;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/v1/files")
public class AwsController {
    private final AwsService awsService;

    @PostMapping("/presigned-url")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> getPresignedUrl(@Valid @RequestBody MetadataDTO metadataDTO) {
        return new ResponseEntity<>(awsService.getPresignedUrl(metadataDTO.category(), metadataDTO.subCategory(), metadataDTO.primaryKey(), metadataDTO.httpMethod()), HttpStatus.OK);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<List<FileUploadResult>> save(@AuthenticationPrincipal UserDetails userDetails, @RequestPart("file") List<Part> parts) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));
        return awsService.save(isAdmin, parts);
    }
}
