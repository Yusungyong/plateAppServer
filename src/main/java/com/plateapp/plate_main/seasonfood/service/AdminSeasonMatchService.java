package com.plateapp.plate_main.seasonfood.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.ConfirmMatchRequest;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.KeywordItem;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.KeywordListResponse;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.KeywordUpsertRequest;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.MatchAdminItem;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.MatchDetailResponse;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.MatchEvidenceItem;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.MatchListResponse;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.RejectMatchRequest;
import com.plateapp.plate_main.seasonfood.repository.AdminSeasonMatchRepository;
import com.plateapp.plate_main.seasonfood.repository.AdminSeasonMatchRepository.KeywordRow;
import com.plateapp.plate_main.seasonfood.repository.AdminSeasonMatchRepository.MatchAdminRow;
import com.plateapp.plate_main.seasonfood.repository.SeasonFoodRepository;
import com.plateapp.plate_main.seasonfood.repository.SeasonStoreMatchRepository;

@Service
public class AdminSeasonMatchService {

  private static final Set<String> KEYWORD_TYPES = Set.of("BASE", "ALIAS", "DISH", "MENU", "EXCLUDE");
  private static final Set<String> MATCH_STATUSES = Set.of("AUTO", "CONFIRMED", "REJECTED");
  private static final Set<String> MATCH_SOURCES = Set.of("FP320_MENU", "RESTAURANT_MENU", "FEED", "VIDEO", "MIXED", "MANUAL");
  private static final int DEFAULT_PAGE = 0;
  private static final int DEFAULT_SIZE = 20;
  private static final int MAX_SIZE = 50;
  private static final BigDecimal DEFAULT_WEIGHT = BigDecimal.ONE;

  private final AdminSeasonMatchRepository adminRepository;
  private final SeasonStoreMatchRepository matchRepository;
  private final SeasonFoodRepository seasonFoodRepository;

  public AdminSeasonMatchService(
      AdminSeasonMatchRepository adminRepository,
      SeasonStoreMatchRepository matchRepository,
      SeasonFoodRepository seasonFoodRepository
  ) {
    this.adminRepository = adminRepository;
    this.matchRepository = matchRepository;
    this.seasonFoodRepository = seasonFoodRepository;
  }

  @Transactional(readOnly = true)
  public KeywordListResponse getKeywords(String ingredientId, String keywordType, String keyword, Integer page, Integer size) {
    assertMatchReady();
    String resolvedType = normalizeKeywordTypeOrNull(keywordType);
    int resolvedPage = normalizePage(page);
    int resolvedSize = normalizeSize(size);

    List<KeywordRow> rows = adminRepository.findKeywords(
        blankToNull(ingredientId),
        resolvedType,
        blankToNull(keyword),
        resolvedSize + 1,
        resolvedPage * resolvedSize
    );

    boolean hasNext = rows.size() > resolvedSize;
    List<KeywordItem> items = rows.stream()
        .limit(resolvedSize)
        .map(this::toKeywordItem)
        .toList();

    return new KeywordListResponse(items, resolvedPage, resolvedSize, hasNext);
  }

  @Transactional
  public KeywordItem createKeyword(KeywordUpsertRequest request) {
    assertMatchReady();
    KeywordInput input = normalizeKeywordInput(request);
    validateIngredient(input.ingredientId());
    validateDuplicateKeyword(input, null);

    Long keywordId = adminRepository.insertKeyword(
        input.ingredientId(),
        input.keyword(),
        input.keywordType(),
        input.matchWeight(),
        input.exactMatch(),
        input.description()
    );

    return findKeywordOrThrow(keywordId);
  }

  @Transactional
  public KeywordItem updateKeyword(Long keywordId, KeywordUpsertRequest request) {
    assertMatchReady();
    if (keywordId == null || keywordId < 1) {
      throw new AppException(ErrorCode.SEASON_KEYWORD_INVALID, "keywordId is invalid");
    }
    KeywordInput input = normalizeKeywordInput(request);
    validateIngredient(input.ingredientId());
    validateDuplicateKeyword(input, keywordId);

    int updated = adminRepository.updateKeyword(
        keywordId,
        input.ingredientId(),
        input.keyword(),
        input.keywordType(),
        input.matchWeight(),
        input.exactMatch(),
        input.description()
    );
    if (updated == 0) {
      throw new AppException(ErrorCode.SEASON_KEYWORD_INVALID, "Season match keyword not found");
    }

    return findKeywordOrThrow(keywordId);
  }

  @Transactional
  public void disableKeyword(Long keywordId) {
    assertMatchReady();
    if (keywordId == null || keywordId < 1 || adminRepository.disableKeyword(keywordId) == 0) {
      throw new AppException(ErrorCode.SEASON_KEYWORD_INVALID, "Season match keyword not found");
    }
  }

  @Transactional(readOnly = true)
  public MatchListResponse getMatches(
      String ingredientId,
      String status,
      String source,
      String keyword,
      Integer page,
      Integer size
  ) {
    assertMatchReady();
    String resolvedStatus = normalizeEnumOrNull(status, MATCH_STATUSES, "status is invalid");
    String resolvedSource = normalizeEnumOrNull(source, MATCH_SOURCES, "source is invalid");
    int resolvedPage = normalizePage(page);
    int resolvedSize = normalizeSize(size);

    List<MatchAdminRow> rows = adminRepository.findMatches(
        blankToNull(ingredientId),
        resolvedStatus,
        resolvedSource,
        blankToNull(keyword),
        resolvedSize + 1,
        resolvedPage * resolvedSize
    );

    boolean hasNext = rows.size() > resolvedSize;
    List<MatchAdminItem> items = rows.stream()
        .limit(resolvedSize)
        .map(this::toMatchAdminItem)
        .toList();

    return new MatchListResponse(items, resolvedPage, resolvedSize, hasNext);
  }

  @Transactional(readOnly = true)
  public MatchDetailResponse getMatchDetail(Long matchId) {
    assertMatchReady();
    MatchAdminRow match = findMatchOrThrow(matchId);
    List<MatchEvidenceItem> evidence = adminRepository.findEvidence(matchId).stream()
        .map(row -> new MatchEvidenceItem(
            row.evidenceId(),
            row.targetType(),
            row.targetId(),
            row.matchedField(),
            row.matchedKeyword(),
            row.matchedText(),
            row.fieldWeight(),
            row.keywordWeight(),
            row.score(),
            row.createdAt()
        ))
        .toList();
    return new MatchDetailResponse(toMatchAdminItem(match), evidence);
  }

  @Transactional
  public MatchDetailResponse confirmMatch(Long matchId, ConfirmMatchRequest request, String username) {
    assertMatchReady();
    MatchAdminRow before = findMatchOrThrow(matchId);
    String menuName = request == null ? null : request.representativeMenuName();
    String imageUrl = request == null ? null : request.representativeImageUrl();
    String note = request == null ? null : request.note();

    if (adminRepository.confirmMatch(matchId, menuName, imageUrl) == 0) {
      throw new AppException(ErrorCode.SEASON_STORE_MATCH_NOT_FOUND, "Season store match not found");
    }
    adminRepository.insertOverride(before, "CONFIRMED", menuName, imageUrl, note, username);
    return getMatchDetail(matchId);
  }

  @Transactional
  public MatchDetailResponse rejectMatch(Long matchId, RejectMatchRequest request, String username) {
    assertMatchReady();
    MatchAdminRow before = findMatchOrThrow(matchId);
    String note = request == null ? null : request.note();

    if (adminRepository.rejectMatch(matchId) == 0) {
      throw new AppException(ErrorCode.SEASON_STORE_MATCH_NOT_FOUND, "Season store match not found");
    }
    adminRepository.insertOverride(before, "REJECTED", null, null, note, username);
    return getMatchDetail(matchId);
  }

  private void assertMatchReady() {
    if (!matchRepository.matchTablesReady()) {
      throw new AppException(ErrorCode.SEASON_STORE_MATCH_NOT_READY, "Season store match tables are not ready");
    }
  }

  private KeywordItem findKeywordOrThrow(Long keywordId) {
    return adminRepository.findKeyword(keywordId).stream()
        .findFirst()
        .map(this::toKeywordItem)
        .orElseThrow(() -> new AppException(ErrorCode.SEASON_KEYWORD_INVALID, "Season match keyword not found"));
  }

  private MatchAdminRow findMatchOrThrow(Long matchId) {
    if (matchId == null || matchId < 1) {
      throw new AppException(ErrorCode.SEASON_STORE_MATCH_NOT_FOUND, "Season store match not found");
    }
    return adminRepository.findMatch(matchId).stream()
        .findFirst()
        .orElseThrow(() -> new AppException(ErrorCode.SEASON_STORE_MATCH_NOT_FOUND, "Season store match not found"));
  }

  private KeywordInput normalizeKeywordInput(KeywordUpsertRequest request) {
    if (request == null) {
      throw new AppException(ErrorCode.SEASON_KEYWORD_INVALID, "request body is required");
    }
    String ingredientId = requireText(request.ingredientId(), "ingredientId is required");
    String keyword = requireText(request.keyword(), "keyword is required");
    String keywordType = normalizeKeywordTypeOrThrow(request.keywordType());
    BigDecimal matchWeight = request.matchWeight() == null ? DEFAULT_WEIGHT : request.matchWeight();
    if (matchWeight.compareTo(BigDecimal.ZERO) < 0) {
      throw new AppException(ErrorCode.SEASON_KEYWORD_INVALID, "matchWeight must be greater than or equal to 0");
    }
    boolean exactMatch = Boolean.TRUE.equals(request.exactMatch());
    return new KeywordInput(ingredientId, keyword, keywordType, matchWeight, exactMatch, blankToNull(request.description()));
  }

  private void validateIngredient(String ingredientId) {
    if (seasonFoodRepository.findIngredient(ingredientId).isEmpty()) {
      throw new AppException(ErrorCode.SEASON_FOOD_NOT_FOUND, "Season food ingredient not found");
    }
  }

  private void validateDuplicateKeyword(KeywordInput input, Long excludeKeywordId) {
    if (adminRepository.existsActiveKeyword(
        input.ingredientId(),
        input.keyword(),
        input.keywordType(),
        excludeKeywordId
    )) {
      throw new AppException(ErrorCode.SEASON_KEYWORD_DUPLICATED, "Season match keyword is duplicated");
    }
  }

  private KeywordItem toKeywordItem(KeywordRow row) {
    return new KeywordItem(
        row.keywordId(),
        row.ingredientId(),
        row.ingredientName(),
        row.keyword(),
        row.keywordType(),
        row.matchWeight(),
        row.exactMatch(),
        row.description(),
        row.useYn(),
        row.createdAt(),
        row.updatedAt()
    );
  }

  private MatchAdminItem toMatchAdminItem(MatchAdminRow row) {
    return new MatchAdminItem(
        row.matchId(),
        row.ingredientId(),
        row.ingredientName(),
        row.storeId(),
        row.restaurantId(),
        row.placeId(),
        row.storeName(),
        row.representativeMenuName(),
        row.matchScore(),
        row.seasonScore(),
        row.evidenceCount(),
        row.matchStatus(),
        row.matchSource(),
        row.matchedKeywords(),
        row.generatedAt(),
        row.expiresAt()
    );
  }

  private String normalizeKeywordTypeOrNull(String keywordType) {
    if (keywordType == null || keywordType.isBlank()) {
      return null;
    }
    return normalizeKeywordTypeOrThrow(keywordType);
  }

  private String normalizeKeywordTypeOrThrow(String keywordType) {
    String normalized = requireText(keywordType, "keywordType is required").toUpperCase();
    if (!KEYWORD_TYPES.contains(normalized)) {
      throw new AppException(ErrorCode.SEASON_KEYWORD_INVALID, "keywordType is invalid");
    }
    return normalized;
  }

  private String normalizeEnumOrNull(String value, Set<String> allowed, String message) {
    if (value == null || value.isBlank()) {
      return null;
    }
    String normalized = value.trim().toUpperCase();
    if (!allowed.contains(normalized)) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, message);
    }
    return normalized;
  }

  private int normalizePage(Integer page) {
    int resolvedPage = page == null ? DEFAULT_PAGE : page;
    if (resolvedPage < 0) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "page must be greater than or equal to 0");
    }
    return resolvedPage;
  }

  private int normalizeSize(Integer size) {
    int resolvedSize = size == null ? DEFAULT_SIZE : size;
    if (resolvedSize < 1 || resolvedSize > MAX_SIZE) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "size must be between 1 and 50");
    }
    return resolvedSize;
  }

  private String requireText(String value, String message) {
    String normalized = blankToNull(value);
    if (normalized == null) {
      throw new AppException(ErrorCode.SEASON_KEYWORD_INVALID, message);
    }
    return normalized;
  }

  private String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private record KeywordInput(
      String ingredientId,
      String keyword,
      String keywordType,
      BigDecimal matchWeight,
      boolean exactMatch,
      String description
  ) {}
}
