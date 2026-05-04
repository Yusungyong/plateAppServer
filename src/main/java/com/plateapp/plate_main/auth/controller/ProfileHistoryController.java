package com.plateapp.plate_main.auth.controller;

import com.plateapp.plate_main.auth.dto.ProfileHistoryRequest;
import com.plateapp.plate_main.auth.dto.ProfileHistoryResponse;
import com.plateapp.plate_main.auth.service.ProfileHistoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class ProfileHistoryController {

    private final ProfileHistoryService service;

    @PostMapping("/{username}/profile-history")
    public ResponseEntity<ProfileHistoryResponse> record(
            @PathVariable("username") String username,
            @Valid @RequestBody ProfileHistoryRequest request,
            Authentication authentication
    ) {
        requireAdmin(authentication);
        return ResponseEntity.ok(service.record(username, request));
    }

    private void requireAdmin(Authentication authentication) {
        boolean isAdmin = authentication != null
                && authentication.getAuthorities() != null
                && authentication.getAuthorities().stream()
                .map(Object::toString)
                .anyMatch("ROLE_ADMIN"::equals);
        if (!isAdmin) {
            throw new AccessDeniedException("Admin role required");
        }
    }
}
