package com.plateapp.plate_main.admin.qna.controller;

import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.qna.dto.QnaListResponse;
import com.plateapp.plate_main.qna.dto.QnaResponse;
import com.plateapp.plate_main.qna.dto.QnaUpdateRequest;
import com.plateapp.plate_main.qna.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/qna")
@RequiredArgsConstructor
public class AdminQnaController {

    private final QnaService qnaService;

    @GetMapping
    public ApiResponse<QnaListResponse> list(
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) String statusCode,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication
    ) {
        requireQnaManage(authentication);
        return ApiResponse.ok(qnaService.listAdminQna(category, statusCode, visibility, page, size));
    }

    @GetMapping("/{qnaId}")
    public ApiResponse<QnaResponse> detail(
            @PathVariable Integer qnaId,
            Authentication authentication
    ) {
        requireQnaManage(authentication);
        return ApiResponse.ok(qnaService.getQna(qnaId, true));
    }

    @PatchMapping("/{qnaId}")
    public ApiResponse<QnaResponse> update(
            @PathVariable Integer qnaId,
            @Valid @RequestBody QnaUpdateRequest request,
            Authentication authentication
    ) {
        requireQnaManage(authentication);
        return ApiResponse.ok(qnaService.updateQna(qnaId, request));
    }

    private void requireQnaManage(Authentication authentication) {
        if (!PlateAuthorities.hasAdminPermission(authentication, PlateAuthorities.PERMISSION_QNA_MANAGE)) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN, "QNA manage permission required");
        }
    }
}
