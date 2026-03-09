// src/main/java/com/plateapp/plate_main/video/controller/HomeVideoController.java
package com.plateapp.plate_main.video.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.Valid;

import com.plateapp.plate_main.video.dto.HomeVideoThumbnailDTO;
import com.plateapp.plate_main.video.dto.VideoFeedItemDTO;
import com.plateapp.plate_main.video.dto.VideoWatchHistoryCreateRequest;
import com.plateapp.plate_main.video.service.HomeVideoService;

import lombok.RequiredArgsConstructor;

@RestController
@Validated
@RequiredArgsConstructor
@RequestMapping("/api/home")
public class HomeVideoController {

    private final HomeVideoService homeVideoService;

    // ??
    //  - ??  /api/home/video-thumbnails?page=0&size=5&username=yoou&isGuest=false
    //  - ?? ?: /api/home/video-thumbnails?...&placeIds=PID1&placeIds=PID2
    @GetMapping("/video-thumbnails")
    public Page<HomeVideoThumbnailDTO> getHomeVideoThumbnails(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "10") int size,
            @RequestParam(name = "sortType", defaultValue = "RECENT") String sortType,
            @RequestParam(name = "lat", required = false) Double lat,
            @RequestParam(name = "lng", required = false) Double lng,
            @RequestParam(name = "radius", required = false) Double radius,
            @RequestParam(name = "username", required = false) String username,
            @RequestParam(name = "isGuest", defaultValue = "false") boolean isGuest,
            @RequestParam(name = "guestId", required = false) String guestId,
            @RequestParam(name = "placeIds", required = false) List<String> placeIds
    ) {
        return homeVideoService.getHomeVideoThumbnails(
                page,
                size,
                sortType,
                lat,
                lng,
                radius,
                username,
                isGuest,
                guestId,
                placeIds
        );
    }

    // ?? ?? ??? ??? API
    @PostMapping("/video-watch-history")
    public ResponseEntity<Void> createVideoWatchHistory(
            @RequestBody @Valid VideoWatchHistoryCreateRequest request
    ) {
        homeVideoService.saveWatchHistory(request);
        return ResponseEntity.ok().build();
    }

    // ?? ?? ?? ??? API (???)
    @GetMapping("/feed")
    public ResponseEntity<List<VideoFeedItemDTO>> getVideoFeed(
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "isGuest", required = false) Boolean isGuest,
            @RequestParam(value = "guestId", required = false) String guestId,
            @RequestParam(value = "storeId", required = false) Integer storeId,
            @RequestParam("placeId") String placeId
    ) {
        List<VideoFeedItemDTO> result =
                homeVideoService.getVideoFeed(resolveUsername(username, isGuest, guestId), storeId, placeId);

        return ResponseEntity.ok(result);
    }

    private String resolveUsername(String usernameParam, Boolean isGuest, String guestId) {
        if (Boolean.TRUE.equals(isGuest)) {
            return null;
        }
        return usernameParam;
    }
}
