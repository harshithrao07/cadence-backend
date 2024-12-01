package com.project.musicplayer.dto;

public record ApiResponseDTO<T>(
        boolean success,
        String message,
        T data
) {
}
