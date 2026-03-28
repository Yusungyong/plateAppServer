package com.plateapp.plate_main.qna.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record QnaCreateRequest(
    @NotBlank @Size(max = 50) String category,
    @NotBlank String question,
    @Size(max = 100) String guestName,
    @Email @Size(max = 255) String guestEmail,
    Boolean isPublic
) {
}
