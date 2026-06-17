package com.plateapp.plate_main.owner.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.owner.service.OwnerStoreService;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/owner/stores")
@RequiredArgsConstructor
public class OwnerStoreController {

    private final OwnerStoreService ownerStoreService;

    @GetMapping
    public ApiResponse<RestaurantAdminDtos.RestaurantListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ApiResponse.ok(ownerStoreService.list(username(authentication), page, size));
    }

    @GetMapping("/{storeId}")
    public ApiResponse<RestaurantAdminDtos.RestaurantDetailResponse> detail(
            @PathVariable Long storeId,
            Authentication authentication
    ) {
        return ApiResponse.ok(ownerStoreService.detail(username(authentication), storeId));
    }

    @PutMapping("/{storeId}")
    public ApiResponse<RestaurantAdminDtos.RestaurantIdResponse> update(
            @PathVariable Long storeId,
            @Valid @RequestBody RestaurantAdminDtos.RestaurantUpsertRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(ownerStoreService.update(username(authentication), storeId, request));
    }

    private String username(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
