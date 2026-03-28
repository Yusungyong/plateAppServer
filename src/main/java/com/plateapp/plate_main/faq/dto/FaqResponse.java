package com.plateapp.plate_main.faq.dto;

import java.time.LocalDateTime;

public record FaqResponse(
    Integer faqId,
    String category,
    String title,
    String answer,
    String username,
    boolean isPinned,
    int viewCount,
    Integer displayOrder,
    String statusCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
