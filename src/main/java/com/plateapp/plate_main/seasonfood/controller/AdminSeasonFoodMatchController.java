package com.plateapp.plate_main.seasonfood.controller;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.ConfirmMatchRequest;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.KeywordItem;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.KeywordListResponse;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.KeywordUpsertRequest;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.MatchDetailResponse;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.MatchListResponse;
import com.plateapp.plate_main.seasonfood.dto.AdminSeasonMatchDtos.RejectMatchRequest;
import com.plateapp.plate_main.seasonfood.service.AdminSeasonMatchService;

@RestController
@RequestMapping("/api/admin")
public class AdminSeasonFoodMatchController {

  private final AdminSeasonMatchService adminSeasonMatchService;

  public AdminSeasonFoodMatchController(AdminSeasonMatchService adminSeasonMatchService) {
    this.adminSeasonMatchService = adminSeasonMatchService;
  }

  @GetMapping("/season-match-keywords")
  public ApiResponse<KeywordListResponse> getKeywords(
      @RequestParam(name = "ingredientId", required = false) String ingredientId,
      @RequestParam(name = "keywordType", required = false) String keywordType,
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size
  ) {
    return ApiResponse.ok(adminSeasonMatchService.getKeywords(ingredientId, keywordType, keyword, page, size));
  }

  @PostMapping("/season-match-keywords")
  public ApiResponse<KeywordItem> createKeyword(@RequestBody KeywordUpsertRequest request) {
    return ApiResponse.ok(adminSeasonMatchService.createKeyword(request));
  }

  @PutMapping("/season-match-keywords/{keywordId}")
  public ApiResponse<KeywordItem> updateKeyword(
      @PathVariable("keywordId") Long keywordId,
      @RequestBody KeywordUpsertRequest request
  ) {
    return ApiResponse.ok(adminSeasonMatchService.updateKeyword(keywordId, request));
  }

  @DeleteMapping("/season-match-keywords/{keywordId}")
  public ApiResponse<Void> deleteKeyword(@PathVariable("keywordId") Long keywordId) {
    adminSeasonMatchService.disableKeyword(keywordId);
    return ApiResponse.ok();
  }

  @GetMapping("/season-store-matches")
  public ApiResponse<MatchListResponse> getMatches(
      @RequestParam(name = "ingredientId", required = false) String ingredientId,
      @RequestParam(name = "status", required = false) String status,
      @RequestParam(name = "source", required = false) String source,
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(name = "page", required = false) Integer page,
      @RequestParam(name = "size", required = false) Integer size
  ) {
    return ApiResponse.ok(adminSeasonMatchService.getMatches(ingredientId, status, source, keyword, page, size));
  }

  @GetMapping("/season-store-matches/{matchId}")
  public ApiResponse<MatchDetailResponse> getMatchDetail(@PathVariable("matchId") Long matchId) {
    return ApiResponse.ok(adminSeasonMatchService.getMatchDetail(matchId));
  }

  @PostMapping("/season-store-matches/{matchId}/confirm")
  public ApiResponse<MatchDetailResponse> confirmMatch(
      @PathVariable("matchId") Long matchId,
      @RequestBody(required = false) ConfirmMatchRequest request,
      Authentication authentication
  ) {
    return ApiResponse.ok(adminSeasonMatchService.confirmMatch(matchId, request, username(authentication)));
  }

  @PostMapping("/season-store-matches/{matchId}/reject")
  public ApiResponse<MatchDetailResponse> rejectMatch(
      @PathVariable("matchId") Long matchId,
      @RequestBody(required = false) RejectMatchRequest request,
      Authentication authentication
  ) {
    return ApiResponse.ok(adminSeasonMatchService.rejectMatch(matchId, request, username(authentication)));
  }

  private String username(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    String name = authentication.getName();
    if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
      return null;
    }
    return name;
  }
}
