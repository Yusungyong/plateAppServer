package com.plateapp.plate_main.admin.feedmoderation.dto;
import jakarta.validation.constraints.*;
import java.time.*;
public final class AdminFeedDtos {
 private AdminFeedDtos(){}
 public record Response(Integer feedId,String username,String title,String content,String images,String storeName,String placeId,
   String visibilityStatus,boolean recommended,long reportCount,String moderationReason,Integer moderatedBy,
   OffsetDateTime moderatedAt,LocalDateTime createdAt,LocalDateTime updatedAt,Long version){}
 public record ActionRequest(@NotBlank @Size(max=1000)String reason,@NotNull Long version){}
 public record RecommendationRequest(boolean recommended,@NotBlank @Size(max=1000)String reason,@NotNull Long version){}
 public record ReportResponse(Integer id,String reporterUsername,Integer reporterUserId,String reason,LocalDateTime submittedAt,String status){}
}
