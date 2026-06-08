package com.plateapp.plate_main.seasonfood.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public class AdminSeasonMatchDtos {

  private AdminSeasonMatchDtos() {
  }

  public record KeywordListResponse(
      List<KeywordItem> items,
      int page,
      int size,
      boolean hasNext
  ) {}

  public record KeywordItem(
      Long keywordId,
      String ingredientId,
      String ingredientName,
      String keyword,
      String keywordType,
      BigDecimal matchWeight,
      boolean exactMatch,
      String description,
      String useYn,
      OffsetDateTime createdAt,
      OffsetDateTime updatedAt
  ) {}

  public record KeywordUpsertRequest(
      String ingredientId,
      String keyword,
      String keywordType,
      BigDecimal matchWeight,
      Boolean exactMatch,
      String description
  ) {}

  public record MatchListResponse(
      List<MatchAdminItem> items,
      int page,
      int size,
      boolean hasNext
  ) {}

  public record MatchAdminItem(
      Long matchId,
      String ingredientId,
      String ingredientName,
      Integer storeId,
      Long restaurantId,
      String placeId,
      String storeName,
      String representativeMenuName,
      BigDecimal matchScore,
      Integer seasonScore,
      int evidenceCount,
      String matchStatus,
      String matchSource,
      String matchedKeywords,
      OffsetDateTime generatedAt,
      OffsetDateTime expiresAt
  ) {}

  public record MatchDetailResponse(
      MatchAdminItem match,
      List<MatchEvidenceItem> evidence
  ) {}

  public record MatchEvidenceItem(
      Long evidenceId,
      String targetType,
      String targetId,
      String matchedField,
      String matchedKeyword,
      String matchedText,
      BigDecimal fieldWeight,
      BigDecimal keywordWeight,
      BigDecimal score,
      OffsetDateTime createdAt
  ) {}

  public record ConfirmMatchRequest(
      String representativeMenuName,
      String representativeImageUrl,
      String note
  ) {}

  public record RejectMatchRequest(
      String note
  ) {}
}
