package com.plateapp.plate_main.owner.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.owner.dto.OwnerApplicationDtos;
import com.plateapp.plate_main.owner.service.OwnerStoreApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/business/applications")
@RequiredArgsConstructor
public class BusinessApplicationController {

    private final OwnerStoreApplicationService applicationService;

    @GetMapping("/{applicationId}/history")
    public ApiResponse<OwnerApplicationDtos.ApplicationHistoryResponse> history(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.history(username(authentication), applicationId));
    }

    @PostMapping("/{applicationId}/resubmit")
    public ApiResponse<OwnerApplicationDtos.SubmitResponse> resubmit(
            @PathVariable Long applicationId,
            @Valid @RequestBody OwnerApplicationDtos.SubmitRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.resubmit(username(authentication), applicationId, request));
    }

    private String username(Authentication authentication) {
        if (authentication == null || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }
        return authentication.getName();
    }
}
