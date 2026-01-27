package com.project.cadence.dto.s3;

import jakarta.validation.constraints.NotBlank;

public record SaveFileDTO(
        @NotBlank(message = "Category cannot be blank") String category,
        @NotBlank(message = "Sub-category cannot be blank") String subCategory,
        @NotBlank(message = "Primary Key cannot be blank") String primaryKey
) {
}
