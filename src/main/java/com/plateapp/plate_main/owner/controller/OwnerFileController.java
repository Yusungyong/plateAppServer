package com.plateapp.plate_main.owner.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import com.plateapp.plate_main.restaurant.service.RestaurantAdminFileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/owner/files")
@RequiredArgsConstructor
public class OwnerFileController {

    private final RestaurantAdminFileService restaurantAdminFileService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RestaurantAdminDtos.RestaurantFileUploadResponse> upload(
            @RequestPart("file") MultipartFile file,
            Authentication authentication
    ) {
        requireAuthenticated(authentication);
        return ApiResponse.ok(restaurantAdminFileService.upload(file));
    }

    private void requireAuthenticated(Authentication authentication) {
        if (authentication == null
                || authentication.getName() == null
                || authentication.getName().isBlank()
                || "anonymousUser".equalsIgnoreCase(authentication.getName())) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
    }
}
