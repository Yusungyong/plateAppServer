package com.plateapp.plate_main.admin.storeapproval.controller;

import com.plateapp.plate_main.admin.security.AdminActor;
import com.plateapp.plate_main.admin.security.AdminActorResolver;
import com.plateapp.plate_main.admin.storeapproval.dto.StoreApprovalDtos;
import com.plateapp.plate_main.admin.storeapproval.service.StoreApprovalService;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/store-approvals")
@RequiredArgsConstructor
public class StoreApprovalController {

    private final StoreApprovalService storeApprovalService;
    private final AdminActorResolver actorResolver;

    @GetMapping
    public ApiResponse<StoreApprovalDtos.ListResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String verificationStatus,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate appliedTo,
            @RequestParam(defaultValue = "appliedAt,desc") String sort,
            Authentication authentication
    ) {
        requirePermission(authentication, PlateAuthorities.PERMISSION_STORE_READ);
        return ApiResponse.ok(storeApprovalService.list(
                page,
                size,
                keyword,
                region,
                category,
                status,
                verificationStatus,
                appliedFrom,
                appliedTo,
                sort
        ));
    }

    @GetMapping("/{applicationId}")
    public ApiResponse<StoreApprovalDtos.DetailResponse> detail(
            @PathVariable Long applicationId,
            Authentication authentication,
            HttpServletRequest request
    ) {
        requirePermission(authentication, PlateAuthorities.PERMISSION_STORE_READ);
        return ApiResponse.ok(storeApprovalService.detail(applicationId, actorResolver.resolve(authentication), request));
    }

    @GetMapping("/{applicationId}/history")
    public ApiResponse<StoreApprovalDtos.HistoryResponse> history(
            @PathVariable Long applicationId,
            Authentication authentication
    ) {
        requirePermission(authentication, PlateAuthorities.PERMISSION_STORE_READ);
        return ApiResponse.ok(storeApprovalService.history(applicationId));
    }

    @PostMapping("/{applicationId}/approve")
    public ApiResponse<StoreApprovalDtos.ActionResponse> approve(
            @PathVariable Long applicationId,
            @Valid @RequestBody StoreApprovalDtos.ApproveRequest command,
            Authentication authentication,
            HttpServletRequest request
    ) {
        requirePermission(authentication, PlateAuthorities.PERMISSION_STORE_APPROVE);
        return ApiResponse.ok(storeApprovalService.approve(
                applicationId,
                command,
                actorResolver.resolve(authentication),
                request
        ));
    }

    @PostMapping("/{applicationId}/hold")
    public ApiResponse<StoreApprovalDtos.ActionResponse> hold(
            @PathVariable Long applicationId,
            @Valid @RequestBody StoreApprovalDtos.HoldRequest command,
            Authentication authentication,
            HttpServletRequest request
    ) {
        requirePermission(authentication, PlateAuthorities.PERMISSION_STORE_APPROVE);
        return ApiResponse.ok(storeApprovalService.hold(
                applicationId,
                command,
                actorResolver.resolve(authentication),
                request
        ));
    }

    @PostMapping("/{applicationId}/request-changes")
    public ApiResponse<StoreApprovalDtos.ActionResponse> requestChanges(
            @PathVariable Long applicationId,
            @Valid @RequestBody StoreApprovalDtos.RequestChangesRequest command,
            Authentication authentication,
            HttpServletRequest request
    ) {
        requirePermission(authentication, PlateAuthorities.PERMISSION_STORE_APPROVE);
        return ApiResponse.ok(storeApprovalService.requestChanges(
                applicationId,
                command,
                actorResolver.resolve(authentication),
                request
        ));
    }

    @PostMapping("/{applicationId}/reject")
    public ApiResponse<StoreApprovalDtos.ActionResponse> reject(
            @PathVariable Long applicationId,
            @Valid @RequestBody StoreApprovalDtos.RejectRequest command,
            Authentication authentication,
            HttpServletRequest request
    ) {
        requirePermission(authentication, PlateAuthorities.PERMISSION_STORE_APPROVE);
        return ApiResponse.ok(storeApprovalService.reject(
                applicationId,
                command,
                actorResolver.resolve(authentication),
                request
        ));
    }

    @PostMapping("/{applicationId}/documents/{documentId}/access-url")
    public ApiResponse<StoreApprovalDtos.DocumentAccessResponse> documentAccess(
            @PathVariable Long applicationId,
            @PathVariable Long documentId,
            @Valid @RequestBody StoreApprovalDtos.DocumentAccessRequest command,
            Authentication authentication,
            HttpServletRequest request
    ) {
        requirePermission(authentication, PlateAuthorities.PERMISSION_STORE_READ);
        AdminActor actor = actorResolver.resolve(authentication);
        return ApiResponse.ok(storeApprovalService.documentAccess(
                applicationId,
                documentId,
                command.purpose(),
                actor,
                request
        ));
    }

    private void requirePermission(Authentication authentication, String permission) {
        if (!PlateAuthorities.hasAdminPermission(authentication, permission)) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN);
        }
    }
}
