package com.plateapp.plate_main.admin.feedback.controller;

import com.plateapp.plate_main.admin.common.AdminPageResponse;
import com.plateapp.plate_main.admin.feedback.dto.FeedbackDtos;
import com.plateapp.plate_main.admin.feedback.service.FeedbackService;
import com.plateapp.plate_main.admin.security.*;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.common.api.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequiredArgsConstructor
public class FeedbackController {
    private final FeedbackService service;
    private final AdminActorResolver actorResolver;

    @PostMapping("/api/feedback")
    public ApiResponse<FeedbackDtos.Response> create(@Valid @RequestBody FeedbackDtos.CreateRequest request,
            Authentication authentication) {
        return ApiResponse.ok(service.create(request, authentication == null ? null : authentication.getName()));
    }

    @GetMapping("/api/admin/feedback")
    public ApiResponse<AdminPageResponse<FeedbackDtos.Response>> list(Authentication auth,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String keyword, @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        require(auth); return ApiResponse.ok(service.list(page, size, keyword, type, status, from, to));
    }

    @GetMapping("/api/admin/feedback/summary")
    public ApiResponse<FeedbackDtos.Summary> summary(Authentication auth) { require(auth); return ApiResponse.ok(service.summary()); }

    @PatchMapping("/api/admin/feedback/{id}")
    public ApiResponse<FeedbackDtos.Response> update(@PathVariable Long id,
            @Valid @RequestBody FeedbackDtos.UpdateRequest command, Authentication auth, HttpServletRequest request) {
        require(auth); return ApiResponse.ok(service.update(id, command, actorResolver.resolve(auth), request));
    }

    private void require(Authentication auth) {
        if (!PlateAuthorities.hasAdminPermission(auth, PlateAuthorities.PERMISSION_SUPPORT_MANAGE))
            throw new AccessDeniedException("SUPPORT_MANAGE is required");
    }
}
