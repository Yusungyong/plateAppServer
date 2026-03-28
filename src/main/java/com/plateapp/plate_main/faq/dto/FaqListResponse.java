package com.plateapp.plate_main.faq.dto;

import java.util.List;

public record FaqListResponse(
    List<FaqResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
}
