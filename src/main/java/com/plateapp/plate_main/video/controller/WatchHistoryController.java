package com.plateapp.plate_main.video.controller;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.video.dto.WatchHistoryDto;
import com.plateapp.plate_main.video.service.WatchHistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class WatchHistoryController {

    private final WatchHistoryService watchHistoryService;

    @PostMapping("/videos/{storeId}/watch/start")
    public ApiResponse<WatchHistoryDto.StartWatchResponse> startWatch(
            @PathVariable Integer storeId,
            @RequestBody WatchHistoryDto.StartWatchRequest request,
            Authentication authentication
    ) {
        String username = authentication.getName();
        WatchHistoryDto.StartWatchResponse response = watchHistoryService.startWatch(username, storeId, request);
        return ApiResponse.ok(response);
    }

    @PutMapping("/videos/{storeId}/watch/progress")
    public ApiResponse<WatchHistoryDto.UpdateProgressResponse> updateProgress(
            @PathVariable Integer storeId,
            @RequestBody WatchHistoryDto.UpdateProgressRequest request
    ) {
        WatchHistoryDto.UpdateProgressResponse response = watchHistoryService.updateProgress(storeId, request);
        return ApiResponse.ok(response);
    }

    @PostMapping("/videos/{storeId}/watch/complete")
    public ApiResponse<WatchHistoryDto.CompleteWatchResponse> completeWatch(
            @PathVariable Integer storeId,
            @RequestBody WatchHistoryDto.CompleteWatchRequest request
    ) {
        WatchHistoryDto.CompleteWatchResponse response = watchHistoryService.completeWatch(storeId, request);
        return ApiResponse.ok(response);
    }

    @GetMapping("/watch-history")
    public ApiResponse<WatchHistoryDto.PageResponse<WatchHistoryDto.WatchHistoryItemResponse>> getWatchHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "false") boolean completedOnly,
            Authentication authentication
    ) {
        String username = authentication.getName();
        WatchHistoryDto.PageResponse<WatchHistoryDto.WatchHistoryItemResponse> response =
                watchHistoryService.getWatchHistory(username, completedOnly, page, size);
        return ApiResponse.ok(response);
    }

    @GetMapping("/videos/{storeId}/watch-info")
    public ApiResponse<WatchHistoryDto.VideoWatchInfoResponse> getVideoWatchInfo(
            @PathVariable Integer storeId,
            Authentication authentication
    ) {
        String username = authentication.getName();
        WatchHistoryDto.VideoWatchInfoResponse response = watchHistoryService.getVideoWatchInfo(username, storeId);
        return ApiResponse.ok(response);
    }

    @GetMapping("/videos/{storeId}/watch-stats")
    public ApiResponse<WatchHistoryDto.VideoWatchStatsResponse> getVideoWatchStats(
            @PathVariable Integer storeId
    ) {
        WatchHistoryDto.VideoWatchStatsResponse response = watchHistoryService.getVideoWatchStats(storeId);
        return ApiResponse.ok(response);
    }
}
