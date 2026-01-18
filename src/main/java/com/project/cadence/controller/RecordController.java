package com.project.cadence.controller;

import com.project.cadence.dto.ApiResponseDTO;
import com.project.cadence.dto.record.NewRecordDTO;
import com.project.cadence.dto.record.RecordPreviewDTO;
import com.project.cadence.dto.record.UpdateRecordDTO;
import com.project.cadence.model.RecordType;
import com.project.cadence.service.JwtService;
import com.project.cadence.service.RecordService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/record")
public class RecordController {
    private final JwtService jwtService;
    private final RecordService recordService;

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewRecord(HttpServletRequest request, @Validated @RequestBody NewRecordDTO newRecordDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return recordService.addNewRecord(newRecordDTO);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @PutMapping("/update/{recordId}")
    public ResponseEntity<ApiResponseDTO<String>> updateExistingRecord(HttpServletRequest request, @Validated @RequestBody UpdateRecordDTO updateRecordDTO, @PathVariable("recordId") String recordId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return recordService.updateExistingRecord(updateRecordDTO, recordId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @DeleteMapping("/delete/{recordId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingRecord(HttpServletRequest request, @PathVariable("recordId") String recordId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return recordService.deleteExistingRecord(recordId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<List<RecordPreviewDTO>>> getAllRecordsByArtistId(
            @RequestParam String artistId,
            @RequestParam(required = false) RecordType recordType
    ) {
        return recordService.getAllRecordsByArtistId(artistId, recordType);
    }

    @GetMapping("/{recordId}")
    public ResponseEntity<ApiResponseDTO<RecordPreviewDTO>> getRecordById(@PathVariable("recordId") String recordId) {
        return recordService.getRecordById(recordId);
    }

}
