package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.profile.dto.UserContentDtos.UserImageItem;
import com.plateapp.plate_main.profile.dto.UserContentDtos.UserVideoItem;
import com.plateapp.plate_main.profile.service.UserContentService;
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
@RequestMapping("/api/users/{username}")
public class UserContentsController {

    private final UserContentService userContentService;

    @GetMapping("/videos")
    public ResponseEntity<List<UserVideoItem>> userVideos(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safeOffset = Math.max(offset, 0);
        return ResponseEntity.ok(userContentService.findUserVideos(username, safeLimit, safeOffset));
    }

    @GetMapping("/images")
    public ResponseEntity<List<UserImageItem>> userImages(
            @PathVariable String username,
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        int safeLimit = Math.min(Math.max(limit, 1), 100);
        int safeOffset = Math.max(offset, 0);
        return ResponseEntity.ok(userContentService.findUserImages(username, safeLimit, safeOffset));
    }
}
