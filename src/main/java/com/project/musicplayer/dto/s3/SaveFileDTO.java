package com.project.musicplayer.dto.s3;

public record SaveFileDTO(
        String category,
        String name,
        String extension
) {
}
