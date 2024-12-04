package com.project.musicplayer.controller;

import com.project.musicplayer.dto.ApiResponseDTO;
import com.project.musicplayer.dto.releases.NewReleaseDTO;
import com.project.musicplayer.dto.releases.ReleasesPreviewDTO;
import com.project.musicplayer.dto.releases.UpdateReleaseDTO;
import com.project.musicplayer.model.ReleaseType;
import com.project.musicplayer.service.JwtService;
import com.project.musicplayer.service.ReleaseService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/release")
public class ReleaseController {
    private final JwtService jwtService;
    private final ReleaseService releaseService;

    @PostMapping("/admin/add")
    public ResponseEntity<ApiResponseDTO<String>> addNewRelease(HttpServletRequest request, @RequestBody NewReleaseDTO newReleaseDTO) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return releaseService.addNewRelease(newReleaseDTO);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @PutMapping("/admin/update/{releaseId}")
    public ResponseEntity<ApiResponseDTO<String>> updateExistingRelease(HttpServletRequest request, @RequestBody UpdateReleaseDTO updateReleaseDTO, @PathVariable("releaseId") String releaseId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return releaseService.updateExistingRelease(updateReleaseDTO, releaseId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @DeleteMapping("/admin/delete/{releaseId}")
    public ResponseEntity<ApiResponseDTO<Void>> deleteExistingRelease(HttpServletRequest request, @PathVariable("releaseId") String releaseId) {
        if (jwtService.checkIfAdminFromHttpRequest(request)) {
            return releaseService.deleteExistingRelease(releaseId);
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiResponseDTO<>(false, "You are not authorized to perform this operation", null));
    }

    @GetMapping("/all")
    public ResponseEntity<ApiResponseDTO<ReleasesPreviewDTO>> getAllReleases(
            @RequestParam(required = false) String artistId,
            @RequestParam(required = false) ReleaseType releaseType
            ) {
        return releaseService.getAllReleases(artistId, releaseType);
    }

}
