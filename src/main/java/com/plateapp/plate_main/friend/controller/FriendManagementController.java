package com.plateapp.plate_main.friend.controller;

import com.plateapp.plate_main.common.dto.ApiResponse;
import com.plateapp.plate_main.common.dto.PagedResponse;
import com.plateapp.plate_main.friend.dto.*;
import com.plateapp.plate_main.friend.service.FriendManagementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/friends/manage")
@RequiredArgsConstructor
public class FriendManagementController {

    private final FriendManagementService friendService;

    // Friend list
    @GetMapping
    public ApiResponse<PagedResponse<FriendDto>> getFriends(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        String username = authentication.getName();
        PagedResponse<FriendDto> friends = friendService.getFriends(username, limit, offset);
        return ApiResponse.success(friends);
    }

    @GetMapping("/search")
    public ApiResponse<PagedResponse<FriendSearchResultDTO>> searchUsers(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        String username = authentication.getName();
        PagedResponse<FriendSearchResultDTO> results = friendService.searchUsers(q, username, limit, offset);
        return ApiResponse.success(results);
    }

    // Delete friend
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteFriend(
            @PathVariable Integer userId,
            Authentication authentication) {
        String username = authentication.getName();
        friendService.deleteFriend(username, userId);
        return ApiResponse.success(null);
    }

    // Sent friend requests
    @GetMapping("/requests/sent")
    public ApiResponse<PagedResponse<FriendRequestDTO>> getSentRequests(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        String username = authentication.getName();
        PagedResponse<FriendRequestDTO> requests = friendService.getSentRequests(username, limit, offset);
        return ApiResponse.success(requests);
    }

    // Received friend requests
    @GetMapping("/requests/received")
    public ApiResponse<PagedResponse<FriendRequestDTO>> getReceivedRequests(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        String username = authentication.getName();
        PagedResponse<FriendRequestDTO> requests = friendService.getReceivedRequests(username, limit, offset);
        return ApiResponse.success(requests);
    }

    @PostMapping("/requests")
    public ApiResponse<FriendRequestDTO> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        FriendRequestDTO friendRequest = friendService.sendFriendRequest(username, request.getToUserId());
        return ApiResponse.success(friendRequest);
    }

    // Cancel friend request
    @DeleteMapping("/requests/{requestId}")
    public ApiResponse<Void> cancelFriendRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String username = authentication.getName();
        friendService.cancelFriendRequest(requestId, username);
        return ApiResponse.success(null);
    }

    // Accept friend request
    @PutMapping("/requests/{requestId}/accept")
    public ApiResponse<Void> acceptFriendRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String username = authentication.getName();
        friendService.acceptFriendRequest(requestId, username);
        return ApiResponse.success(null);
    }

    // Reject friend request
    @PutMapping("/requests/{requestId}/reject")
    public ApiResponse<Void> rejectFriendRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String username = authentication.getName();
        friendService.rejectFriendRequest(requestId, username);
        return ApiResponse.success(null);
    }
}