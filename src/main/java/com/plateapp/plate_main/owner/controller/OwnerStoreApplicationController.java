package com.plateapp.plate_main.owner.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.owner.dto.OwnerApplicationDtos;
import com.plateapp.plate_main.owner.service.OwnerStoreApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/owner")
@RequiredArgsConstructor
public class OwnerStoreApplicationController {

    private final OwnerStoreApplicationService applicationService;

    @PostMapping("/signup-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OwnerApplicationDtos.ApplicationCreatedResponse> signupAndCreate(
            @Valid @RequestBody OwnerApplicationDtos.SignupApplicationRequest request
    ) {
        return ApiResponse.ok(applicationService.signupAndCreate(request));
    }

    @PostMapping("/store-applications")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OwnerApplicationDtos.ApplicationCreatedResponse> create(
            @Valid @RequestBody OwnerApplicationDtos.StoreApplicationUpsertRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.create(username(authentication), request));
    }

    @GetMapping("/store-applications")
    public ApiResponse<OwnerApplicationDtos.ApplicationListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.list(username(authentication), page, size));
    }

    @GetMapping("/store-applications/{applicationId}")
    public ApiResponse<OwnerApplicationDtos.ApplicationDetailResponse> detail(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.detail(username(authentication), applicationId));
    }

    @GetMapping("/store-applications/{applicationId}/history")
    public ApiResponse<OwnerApplicationDtos.ApplicationHistoryResponse> history(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.history(username(authentication), applicationId));
    }

    @PutMapping("/store-applications/{applicationId}")
    public ApiResponse<OwnerApplicationDtos.ApplicationDetailResponse> update(
            @PathVariable Long applicationId,
            @Valid @RequestBody OwnerApplicationDtos.StoreApplicationUpsertRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.update(username(authentication), applicationId, request));
    }

    @PostMapping(
            value = "/store-applications/{applicationId}/documents",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<OwnerApplicationDtos.DocumentUploadResponse> uploadDocument(
            @PathVariable Long applicationId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("documentType") String documentType,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.uploadDocument(
                username(authentication),
                applicationId,
                documentType,
                file
        ));
    }

    @PostMapping("/store-applications/{applicationId}/submit")
    public ApiResponse<OwnerApplicationDtos.SubmitResponse> submit(
            @PathVariable Long applicationId,
            @Valid @RequestBody OwnerApplicationDtos.SubmitRequest request,
            Authentication authentication
    ) {
        return ApiResponse.ok(applicationService.submit(username(authentication), applicationId, request));
    }

    @PostMapping("/store-applications/{applicationId}/resubmit")
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
