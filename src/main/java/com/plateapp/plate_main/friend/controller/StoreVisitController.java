package com.plateapp.plate_main.friend.controller;

import com.plateapp.plate_main.friend.dto.FriendStoreVisitDto;
import com.plateapp.plate_main.friend.service.FriendService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
            @RequestParam("username") String username
    ) {
        return ResponseEntity.ok(friendService.getStoreFriendVisits(username, storeId));
    }
}
