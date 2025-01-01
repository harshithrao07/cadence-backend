package com.project.musicplayer.controller;

import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.record.NewRecordDTO;
import com.project.musicplayer.dto.record.RecordPreviewDTO;
import com.project.musicplayer.dto.record.UpdateRecordDTO;
import com.project.musicplayer.model.RecordType;
import com.project.musicplayer.service.JwtService;
import com.project.musicplayer.service.RecordService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/record")
public class RecordController {
    private final JwtService jwtService;
    private final RecordService recordService;

    @PostMapping("/admin/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewRecord(HttpServletRequest request, @Valid @RequestBody NewRecordDTO newRecordDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return recordService.addNewRecord(newRecordDTO);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @PutMapping("/admin/update/{recordId}")
    public ResponseEntity<ApiResponseDTO<String>> updateExistingRecord(HttpServletRequest request, @Valid @RequestBody UpdateRecordDTO updateRecordDTO, @PathVariable("recordId") String recordId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return recordService.updateExistingRecord(updateRecordDTO, recordId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @DeleteMapping("/admin/delete/{recordId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingRecord(HttpServletRequest request, @PathVariable("recordId") String recordId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return recordService.deleteExistingRecord(recordId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<Set<RecordPreviewDTO>>> getAllRecordsByArtistId(
            @RequestParam String artistId,
            @RequestParam(required = false) RecordType recordType
    ) {
        return recordService.getAllRecordsByArtistId(artistId, recordType);
    }

}
