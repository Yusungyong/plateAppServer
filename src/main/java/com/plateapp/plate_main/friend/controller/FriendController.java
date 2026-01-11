package com.plateapp.plate_main.friend.controller;

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
            @RequestParam("username") String username,
            @RequestParam(value = "status", required = false) String status
    ) {
        return ResponseEntity.ok(new FriendListResponse(friendService.list(username, status)));
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
            @RequestParam(value = "cursor", required = false) Integer cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(friendService.listVisits(username, cursor, limit));
    }

    @GetMapping("/{username}/recent-stores")
    public ResponseEntity<RecentStoreResponse> recentStores(
            @PathVariable("username") String username,
            @RequestParam(value = "limit", defaultValue = "3") int limit
    ) {
        return ResponseEntity.ok(friendService.listRecentStores(username, limit));
    }

    @GetMapping("/stores/{storeId}/friend-visits")
    public ResponseEntity<FriendStoreVisitDto> storeFriendVisits(
            @PathVariable("storeId") Integer storeId,
            @RequestParam("username") String username
    ) {
        return ResponseEntity.ok(friendService.getStoreFriendVisits(username, storeId));
    }

    @PostMapping
    public ResponseEntity<FriendDto> add(@RequestBody CreateFriendRequest request) {
        return ResponseEntity.ok(friendService.add(request));
    }

    @GetMapping("/{username}/{friendName}/visits")
    public ResponseEntity<FriendVisitResponse> friendVisits(
            @PathVariable("username") String username,
            @PathVariable("friendName") String friendName,
            @RequestParam(value = "cursor", required = false) Integer cursor,
            @RequestParam(value = "limit", defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(friendService.listFriendVisits(username, friendName, cursor, limit));
    }

    @GetMapping("/{username}/scheduled-visits")
    public ResponseEntity<ScheduledVisitResponse> scheduledVisits(
            @PathVariable("username") String username,
            @RequestParam(value = "fromDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(value = "toDate", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return ResponseEntity.ok(friendService.listScheduledVisits(username, fromDate, toDate));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<FriendDto> updateStatus(
            @PathVariable Integer id,
            @RequestBody UpdateStatusRequest request
    ) {
        return ResponseEntity.ok(friendService.updateStatus(id, request.status()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        friendService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
