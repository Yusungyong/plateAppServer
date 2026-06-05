package com.plateapp.plate_main.friend.controller;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.friend.dto.FriendStoreVisitDto;
import com.plateapp.plate_main.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/stores")
public class StoreVisitController {

    private final FriendService friendService;

    @GetMapping("/{storeId}/friend-visits")
    public ResponseEntity<FriendStoreVisitDto> friendVisits(
            @PathVariable("storeId") Integer storeId,
            @RequestParam(value = "username", required = false) String username,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        if (username != null && !username.isBlank() && !username.equals(currentUsername)) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN, "Cannot access another user's friend data");
        }
        return ResponseEntity.ok(friendService.getStoreFriendVisits(currentUsername, storeId));
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
