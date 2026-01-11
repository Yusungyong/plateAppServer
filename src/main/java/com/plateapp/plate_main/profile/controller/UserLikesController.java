package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.profile.dto.LikedContentDtos.LikedImageItem;
import com.plateapp.plate_main.profile.dto.LikedContentDtos.LikedVideoItem;
import com.plateapp.plate_main.profile.service.LikedContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users/{username}/likes")
public class UserLikesController {

    private final LikedContentService likedContentService;

    @GetMapping("/videos")
    public ResponseEntity<List<LikedVideoItem>> likedVideos(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safeOffset = Math.max(offset, 0);
        return ResponseEntity.ok(likedContentService.findLikedVideos(username, safeLimit, safeOffset));
    }

    @GetMapping("/images")
    public ResponseEntity<List<LikedImageItem>> likedImages(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safeOffset = Math.max(offset, 0);
        return ResponseEntity.ok(likedContentService.findLikedImages(username, safeLimit, safeOffset));
    }
}
