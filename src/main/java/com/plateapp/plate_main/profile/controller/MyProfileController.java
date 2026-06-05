package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.profile.dto.MyProfileRequest;
import com.plateapp.plate_main.profile.dto.MyProfileResponse;
import com.plateapp.plate_main.profile.service.MyProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my")
public class MyProfileController {

    private final MyProfileService myProfileService;

    @PostMapping("/profile")
    public ResponseEntity<MyProfileResponse> getProfile(
            @RequestBody @Valid MyProfileRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(myProfileService.getProfile(currentUsername(authentication), request.includeStats()));
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthorized");
        }
        String username = authentication.getName();
        if (username == null || username.isBlank() || "anonymousUser".equalsIgnoreCase(username)) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthorized");
        }
        return username;
    }
}
