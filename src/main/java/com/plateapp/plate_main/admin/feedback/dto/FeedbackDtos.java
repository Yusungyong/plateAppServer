package com.plateapp.plate_main.admin.feedback.dto;

import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;

public final class FeedbackDtos {
    private FeedbackDtos() {}

    public record CreateRequest(
            @NotBlank @Size(max = 40) String type,
            @NotBlank @Size(max = 5000) String content,
            @Size(max = 320) String contact
    ) {}

    public record UpdateRequest(
            @Size(max = 40) String status,
            Integer assigneeUserId,
            @Size(max = 5000) String internalMemo,
            @NotNull Long version
    ) {}

    public record Response(Long id, String type, String content, String status, String contact,
            OffsetDateTime contactPurgeAt, Integer requesterUserId, Integer assigneeUserId, String internalMemo,
            OffsetDateTime createdAt, OffsetDateTime updatedAt, Long version) {}

    public record Summary(long total, long received, long inProgress, long resolved, long improvementCandidates) {}
}
