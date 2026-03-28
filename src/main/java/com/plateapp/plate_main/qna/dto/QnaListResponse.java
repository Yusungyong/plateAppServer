package com.plateapp.plate_main.qna.dto;

import java.util.List;

public record QnaListResponse(
    List<QnaResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages,
    boolean hasNext
) {
}
