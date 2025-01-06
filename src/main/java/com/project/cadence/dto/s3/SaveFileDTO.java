package com.project.cadence.dto.s3;

import jakarta.validation.constraints.NotBlank;

public record SaveFileDTO(
        @NotBlank(message = "Category cannot be blank") String category,
        @NotBlank(message = "Sub-category cannot be blank") String subCategory,
        @NotBlank(message = "Name cannot be blank") String name,
        @NotBlank(message = "Extension cannot be blank") String extension
) {
}
