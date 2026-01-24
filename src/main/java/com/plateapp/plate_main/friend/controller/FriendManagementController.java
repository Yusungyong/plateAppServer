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
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendManagementController {

    private final FriendManagementService friendService;

    // 친구 목록 조회
    @GetMapping
    public ApiResponse<PagedResponse<FriendDTO>> getFriends(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        String username = authentication.getName();
        PagedResponse<FriendDTO> friends = friendService.getFriends(username, limit, offset);
        return ApiResponse.success(friends);
    }

    // 사용자 검색
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

    // 친구 삭제
    @DeleteMapping("/{userId}")
    public ApiResponse<Void> deleteFriend(
            @PathVariable Integer userId,
            Authentication authentication) {
        String username = authentication.getName();
        friendService.deleteFriend(username, userId);
        return ApiResponse.success(null);
    }

    // 보낸 친구 요청 목록
    @GetMapping("/requests/sent")
    public ApiResponse<PagedResponse<FriendRequestDTO>> getSentRequests(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        String username = authentication.getName();
        PagedResponse<FriendRequestDTO> requests = friendService.getSentRequests(username, limit, offset);
        return ApiResponse.success(requests);
    }

    // 받은 친구 요청 목록
    @GetMapping("/requests/received")
    public ApiResponse<PagedResponse<FriendRequestDTO>> getReceivedRequests(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset,
            Authentication authentication) {
        String username = authentication.getName();
        PagedResponse<FriendRequestDTO> requests = friendService.getReceivedRequests(username, limit, offset);
        return ApiResponse.success(requests);
    }

    // 친구 요청 보내기
    @PostMapping("/requests")
    public ApiResponse<FriendRequestDTO> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        FriendRequestDTO friendRequest = friendService.sendFriendRequest(username, request.getToUserId());
        return ApiResponse.success(friendRequest);
    }

    // 친구 요청 취소
    @DeleteMapping("/requests/{requestId}")
    public ApiResponse<Void> cancelFriendRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String username = authentication.getName();
        friendService.cancelFriendRequest(requestId, username);
        return ApiResponse.success(null);
    }

    // 친구 요청 수락
    @PutMapping("/requests/{requestId}/accept")
    public ApiResponse<Void> acceptFriendRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String username = authentication.getName();
        friendService.acceptFriendRequest(requestId, username);
        return ApiResponse.success(null);
    }

    // 친구 요청 거절
    @PutMapping("/requests/{requestId}/reject")
    public ApiResponse<Void> rejectFriendRequest(
            @PathVariable Integer requestId,
            Authentication authentication) {
        String username = authentication.getName();
        friendService.rejectFriendRequest(requestId, username);
        return ApiResponse.success(null);
    }
}
