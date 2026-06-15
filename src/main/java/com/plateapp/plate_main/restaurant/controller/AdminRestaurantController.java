package com.plateapp.plate_main.restaurant.controller;

import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import com.plateapp.plate_main.restaurant.service.RestaurantAdminFileService;
import com.plateapp.plate_main.restaurant.service.RestaurantAdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class AdminRestaurantController {

    private final RestaurantAdminService restaurantAdminService;
    private final RestaurantAdminFileService restaurantAdminFileService;

    @GetMapping("/api/admin/restaurants")
    public RestaurantAdminDtos.RestaurantListResponse listRestaurants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String exposureStatus,
            Authentication authentication
    ) {
        requireRestaurantManage(authentication);
        return restaurantAdminService.listRestaurants(keyword, category, exposureStatus, page, size);
    }

    @GetMapping("/api/admin/restaurants/{restaurantId}")
    public ApiResponse<RestaurantAdminDtos.RestaurantDetailResponse> getRestaurant(
            @PathVariable Long restaurantId,
            Authentication authentication
    ) {
        requireRestaurantManage(authentication);
        return ApiResponse.ok(restaurantAdminService.getRestaurant(restaurantId));
    }

    @PostMapping("/api/admin/restaurants")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RestaurantAdminDtos.RestaurantIdResponse> createRestaurant(
            @Valid @RequestBody RestaurantAdminDtos.RestaurantUpsertRequest request,
            Authentication authentication
    ) {
        requireRestaurantManage(authentication);
        return ApiResponse.ok(restaurantAdminService.createRestaurant(request));
    }

    @PutMapping("/api/admin/restaurants/{restaurantId}")
    public ApiResponse<RestaurantAdminDtos.RestaurantIdResponse> updateRestaurant(
            @PathVariable Long restaurantId,
            @Valid @RequestBody RestaurantAdminDtos.RestaurantUpsertRequest request,
            Authentication authentication
    ) {
        requireRestaurantManage(authentication);
        return ApiResponse.ok(restaurantAdminService.updateRestaurant(restaurantId, request));
    }

    @DeleteMapping("/api/admin/restaurants/{restaurantId}")
    public ApiResponse<RestaurantAdminDtos.RestaurantDeleteResponse> deleteRestaurant(
            @PathVariable Long restaurantId,
            Authentication authentication
    ) {
        requireRestaurantManage(authentication);
        return ApiResponse.ok(restaurantAdminService.deleteRestaurant(restaurantId));
    }

    @PostMapping(value = "/api/admin/files", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RestaurantAdminDtos.RestaurantFileUploadResponse> uploadRestaurantFile(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        requireRestaurantManage(authentication);
        return ApiResponse.ok(restaurantAdminFileService.upload(file));
    }

    private void requireRestaurantManage(Authentication authentication) {
        if (!PlateAuthorities.hasAdminPermission(
                authentication,
                PlateAuthorities.PERMISSION_RESTAURANT_MANAGE
        )) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN, "Restaurant manage permission required.");
        }
    }
}
