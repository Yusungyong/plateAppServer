package com.plateapp.plate_main.friend.controller;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.friend.dto.FriendDto;
import com.plateapp.plate_main.friend.dto.FriendListResponse;
import com.plateapp.plate_main.friend.dto.FriendRequests.CreateFriendRequest;
import com.plateapp.plate_main.friend.dto.FriendRequests.UpdateStatusRequest;
import com.plateapp.plate_main.friend.dto.FriendSearchResponse;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.VisitResponse;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.FriendVisitResponse;
import com.plateapp.plate_main.friend.dto.FriendVisitDtos.ScheduledVisitResponse;
import com.plateapp.plate_main.friend.dto.FriendRecentStoresDto.RecentStoreResponse;
import com.plateapp.plate_main.friend.dto.FriendStoreVisitDto;
import com.plateapp.plate_main.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDate;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    @GetMapping
    public ResponseEntity<FriendListResponse> list(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "status", required = false) String status,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        requireRequestedUser(username, currentUsername);
        return ResponseEntity.ok(new FriendListResponse(friendService.list(currentUsername, status)));
    }

    @GetMapping("/search")
    public ResponseEntity<FriendSearchResponse> search(
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "limit", defaultValue = "10") int limit
    ) {
        return ResponseEntity.ok(new FriendSearchResponse(friendService.search(keyword, limit)));
    }

    @GetMapping("/{username}/visits")
    public ResponseEntity<VisitResponse> visits(
            @PathVariable("username") String username,
            @RequestParam(value = "friendName", required = false) String friendName,
            @RequestParam(value = "cursor", required = false) Integer cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        requireRequestedUser(username, currentUsername);
        return ResponseEntity.ok(friendService.listVisits(currentUsername, friendName, cursor, limit));
    }

    @GetMapping("/{username}/recent-stores")
    public ResponseEntity<RecentStoreResponse> recentStores(
            @PathVariable("username") String username,
            @RequestParam(value = "limit", defaultValue = "3") int limit,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        requireRequestedUser(username, currentUsername);
        return ResponseEntity.ok(friendService.listRecentStores(currentUsername, limit));
    }

    @GetMapping("/suggest")
    public ResponseEntity<FriendListResponse> suggest(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam("keyword") String keyword,
            @RequestParam(value = "status", defaultValue = "cd_002") String status,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            @RequestParam(value = "offset", defaultValue = "0") int offset,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        requireRequestedUser(username, currentUsername);
        return ResponseEntity.ok(new FriendListResponse(friendService.suggest(currentUsername, keyword, status, limit, offset)));
    }

    @GetMapping("/stores/{storeId}/friend-visits")
    public ResponseEntity<FriendStoreVisitDto> storeFriendVisits(
            @PathVariable("storeId") Integer storeId,
            @RequestParam(value = "username", required = false) String username,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        requireRequestedUser(username, currentUsername);
        return ResponseEntity.ok(friendService.getStoreFriendVisits(currentUsername, storeId));
    }

    @GetMapping("/{username}/stores/{storeId}/visits")
    public ResponseEntity<FriendStoreVisitDto> userStoreFriendVisits(
            @PathVariable("username") String username,
            @PathVariable("storeId") Integer storeId,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        requireRequestedUser(username, currentUsername);
        return ResponseEntity.ok(friendService.getStoreFriendVisits(currentUsername, storeId));
    }

    @PostMapping
    public ResponseEntity<FriendDto> add(
            @RequestBody CreateFriendRequest request,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        if (request != null) {
            requireRequestedUser(request.username(), currentUsername);
        }
        return ResponseEntity.ok(friendService.add(currentUsername, request));
    }

    @GetMapping("/{username}/{friendName}/visits")
    public ResponseEntity<FriendVisitResponse> friendVisits(
            @PathVariable("username") String username,
            @PathVariable("friendName") String friendName,
            @RequestParam(value = "cursor", required = false) Integer cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        requireRequestedUser(username, currentUsername);
        return ResponseEntity.ok(friendService.listFriendVisits(currentUsername, friendName, cursor, limit));
    }

    @GetMapping("/{username}/scheduled-visits")
    public ResponseEntity<ScheduledVisitResponse> scheduledVisits(
            @PathVariable("username") String username,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication authentication
    ) {
        String currentUsername = currentUsername(authentication);
        requireRequestedUser(username, currentUsername);
        return ResponseEntity.ok(friendService.listScheduledVisits(currentUsername, fromDate, toDate));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FriendDto> updateStatus(
            @PathVariable Integer id,
            @RequestBody UpdateStatusRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(friendService.updateStatus(id, currentUsername(authentication), request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id, Authentication authentication) {
        friendService.delete(id, currentUsername(authentication));
        return ResponseEntity.noContent().build();
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

    private void requireRequestedUser(String requestedUsername, String currentUsername) {
        if (requestedUsername != null
                && !requestedUsername.isBlank()
                && !requestedUsername.equals(currentUsername)) {
            throw new AppException(ErrorCode.AUTH_FORBIDDEN, "Cannot access another user's friend data");
        }
    }
}
