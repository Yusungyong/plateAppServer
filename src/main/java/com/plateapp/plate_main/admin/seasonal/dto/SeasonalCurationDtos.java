package com.plateapp.plate_main.admin.seasonal.dto;import jakarta.validation.Valid;import jakarta.validation.constraints.*;import java.time.OffsetDateTime;import java.util.List;
public final class SeasonalCurationDtos{private SeasonalCurationDtos(){}
 public record UpsertRequest(@NotBlank @Size(max=150)String title,@Size(max=5000)String description,Integer displayOrder,OffsetDateTime startsAt,OffsetDateTime endsAt,List<Long> storeIds,List<Long> menuIds,Long version){}
 public record PublishRequest(@NotNull Long version){}
 public record OrderItem(@NotNull Long curationId,@NotNull @Min(0)Integer displayOrder,@NotNull Long version){}
 public record OrderRequest(@NotEmpty List<@Valid OrderItem> items){}
 public record Response(Long id,String title,String description,String status,Integer displayOrder,OffsetDateTime startsAt,OffsetDateTime endsAt,List<Long> storeIds,List<Long> menuIds,Integer createdBy,Integer updatedBy,OffsetDateTime createdAt,OffsetDateTime updatedAt,Long version){}
}
