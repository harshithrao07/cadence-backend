package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.record.UpsertRecordDTO;
import com.project.cadence.dto.record.RecordPreviewDTO;
import com.project.cadence.dto.record.UpsertRecordResponseDTO;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.RecordService;
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
@RequestMapping("/api/v1/record")
public class RecordController {
    private final JwtService jwtService;
    private final RecordService recordService;

    @PostMapping("/upsert")
    public ResponseEntity<ApiResponseDTO<UpsertRecordResponseDTO>> upsertNewRecord(HttpServletRequest request, @Validated @RequestBody UpsertRecordDTO upsertRecordDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return recordService.upsertNewRecord(upsertRecordDTO);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @DeleteMapping("/delete/{recordId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteRecord(HttpServletRequest request, @PathVariable("recordId") String recordId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return recordService.deleteRecord(recordId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<List<RecordPreviewDTO>>> getAllRecordsByArtistId(
            @RequestParam String artistId
    ) {
        return recordService.getAllRecordsByArtistId(artistId);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponseDTO<RecordPreviewDTO>> getRecordById(@PathVariable("recordId") String recordId) {
        return recordService.getRecordById(recordId);
    }

}
