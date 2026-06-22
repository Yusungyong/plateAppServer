package com.plateapp.plate_main.admin.storeoperation.dto;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.Map;
public final class AdminStoreDtos {
 private AdminStoreDtos() {}
 public record Response(Long storeId,String title,String address,String phone,String businessHours,String introduction,
  String operationStatus,String visibilityStatus,String lastReason,Integer updatedBy,OffsetDateTime operationUpdatedAt,
  OffsetDateTime createdAt,OffsetDateTime updatedAt,Long version) {}
 public record StatusRequest(@NotBlank @Size(max=30)String status,@NotBlank @Size(max=1000)String reason,@NotNull Long version) {}
 public record HistoryResponse(Long id,OffsetDateTime occurredAt,Integer actorUserId,String actorRole,String action,
  Map<String,Object> previousValue,Map<String,Object> nextValue,String reason) {}
}
