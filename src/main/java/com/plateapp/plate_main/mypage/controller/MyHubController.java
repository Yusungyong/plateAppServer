package com.plateapp.plate_main.mypage.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.mypage.dto.MyHubResponse;
import com.plateapp.plate_main.mypage.service.MyHubService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my")
public class MyHubController {

    static final int DEFAULT_PREVIEW_LIMIT = 3;
    static final int MAX_PREVIEW_LIMIT = 6;

    private final MyHubService myHubService;

    @GetMapping("/hub")
    public ApiResponse<MyHubResponse> getHub(
            @RequestParam(name = "previewLimit", required = false) String previewLimit,
            Authentication authentication
    ) {
        return ApiResponse.ok(myHubService.getHub(
                currentUsername(authentication),
                parsePreviewLimit(previewLimit)
        ));
    }

    private int parsePreviewLimit(String value) {
        if (value == null) {
            return DEFAULT_PREVIEW_LIMIT;
        }
        if (value.isBlank()) {
            throw invalidPreviewLimit();
        }
        final int parsed;
        try {
            parsed = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw invalidPreviewLimit();
        }
        if (parsed < 0 || parsed > MAX_PREVIEW_LIMIT) {
            throw invalidPreviewLimit();
        }
        return parsed;
    }

    private AppException invalidPreviewLimit() {
        return new AppException(
                ErrorCode.COMMON_INVALID_INPUT,
                "previewLimit는 0 이상 6 이하여야 합니다."
        );
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return username;
    }
}
