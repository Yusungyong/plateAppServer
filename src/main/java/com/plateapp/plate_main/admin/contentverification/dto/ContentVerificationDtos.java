package com.plateapp.plate_main.admin.contentverification.dto;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
public final class ContentVerificationDtos {
 private ContentVerificationDtos() {}
 public record Response(Long id,String targetType,String targetId,String status,Integer requesterUserId,
   Integer assigneeUserId,String reviewReason,OffsetDateTime createdAt,OffsetDateTime updatedAt,Long version) {}
 public record AssigneeRequest(@NotNull Integer assigneeUserId,@NotNull Long version) {}
 public record ActionRequest(@NotNull Long version,@Size(max=2000) String reason) {}
 public record HistoryResponse(Long id,String action,String previousStatus,String nextStatus,Integer actorUserId,
   Integer assigneeUserId,String reason,OffsetDateTime createdAt) {}
}
