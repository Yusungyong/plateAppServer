package com.plateapp.plate_main.faq.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FaqUpsertRequest(
    @Size(max = 100) String category,
    @NotBlank @Size(max = 255) String title,
    @NotBlank String answer,
    Boolean isPinned,
    Integer displayOrder,
    @NotBlank @Size(max = 50) String statusCode
) {
}
