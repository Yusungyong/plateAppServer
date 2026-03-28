package com.plateapp.plate_main.qna.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QnaUpdateRequest(
    @NotBlank String answer,
    @NotBlank @Size(max = 20) String statusCode,
    Boolean isPublic
) {
}
