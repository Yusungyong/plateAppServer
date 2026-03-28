package com.plateapp.plate_main.faq.controller;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.faq.dto.FaqListResponse;
import com.plateapp.plate_main.faq.dto.FaqResponse;
import com.plateapp.plate_main.faq.dto.FaqUpsertRequest;
import com.plateapp.plate_main.faq.service.FaqService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/faqs")
@RequiredArgsConstructor
public class FaqController {

    private final FaqService faqService;

    @GetMapping
    public ResponseEntity<FaqListResponse> listFaqs(
        @RequestParam(value = "category", required = false) String category,
        @RequestParam(value = "keyword", required = false) String keyword,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "10") int size
    ) {
        return ResponseEntity.ok(faqService.listFaqs(category, keyword, page, size));
    }

    @GetMapping("/{faqId}")
    public ResponseEntity<FaqResponse> getFaq(@PathVariable Integer faqId) {
        return ResponseEntity.ok(faqService.getFaq(faqId));
    }

    @PostMapping
    public ResponseEntity<FaqResponse> createFaq(
        @Valid @RequestBody FaqUpsertRequest request,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(faqService.createFaq(authentication.getName(), request));
    }

    @PatchMapping("/{faqId}")
    public ResponseEntity<FaqResponse> updateFaq(
        @PathVariable Integer faqId,
        @Valid @RequestBody FaqUpsertRequest request,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(faqService.updateFaq(faqId, request));
    }

    @DeleteMapping("/{faqId}")
    public ResponseEntity<Void> deleteFaq(
        @PathVariable Integer faqId,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        faqService.deleteFaq(faqId);
        return ResponseEntity.noContent().build();
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED, "Unauthorized");
        }

        boolean isAdmin = authentication.getAuthorities().stream()
            .map(GrantedAuthority::getAuthority)
            .anyMatch("ROLE_ADMIN"::equals);

        if (!isAdmin) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN, "Admin role required");
        }
    }
}
