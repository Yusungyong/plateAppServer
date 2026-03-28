package com.plateapp.plate_main.qna.dto;

import java.time.LocalDateTime;

public record QnaResponse(
    Integer qnaId,
    String username,
    String guestName,
    String guestEmail,
    String category,
    String question,
    String answer,
    String statusCode,
    boolean isPublic,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    LocalDateTime answeredAt
) {
}
