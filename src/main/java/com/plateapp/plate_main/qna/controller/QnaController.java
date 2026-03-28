package com.plateapp.plate_main.qna.controller;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.qna.dto.QnaCreateRequest;
import com.plateapp.plate_main.qna.dto.QnaListResponse;
import com.plateapp.plate_main.qna.dto.QnaResponse;
import com.plateapp.plate_main.qna.dto.QnaUpdateRequest;
import com.plateapp.plate_main.qna.service.QnaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/qna")
@RequiredArgsConstructor
public class QnaController {

    private final QnaService qnaService;

    @GetMapping
    public ResponseEntity<QnaListResponse> listQna(
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "statusCode", required = false) String statusCode,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size,
        Authentication authentication
    ) {
        return ResponseEntity.ok(qnaService.listQna(category, statusCode, page, size, isAdmin(authentication)));
    }

    @GetMapping("/{qnaId}")
    public ResponseEntity<QnaResponse> getQna(
        @PathVariable Integer qnaId,
        Authentication authentication
    ) {
        return ResponseEntity.ok(qnaService.getQna(qnaId, isAdmin(authentication)));
    }

    @PostMapping
    public ResponseEntity<QnaResponse> createQna(
        @Valid @RequestBody QnaCreateRequest request,
        Authentication authentication
    ) {
        return ResponseEntity.ok(qnaService.createQna(currentUsername(authentication), request));
    }

    @PatchMapping("/{qnaId}")
    public ResponseEntity<QnaResponse> updateQna(
        @PathVariable Integer qnaId,
        @Valid @RequestBody QnaUpdateRequest request,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(qnaService.updateQna(qnaId, request));
    }

    private String currentUsername(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }
        String name = authentication.getName();
        if (name == null || name.isBlank() || "anonymousUser".equalsIgnoreCase(name)) {
            return null;
        }
        return name;
    }

    private boolean isAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthorized");
        }
        if (!isAdmin(authentication)) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN, "Admin role required");
        }
    }
}
