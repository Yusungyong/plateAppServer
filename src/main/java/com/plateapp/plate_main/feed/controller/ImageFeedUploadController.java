package com.plateapp.plate_main.feed.controller;

import com.plateapp.plate_main.feed.dto.ImageFeedUploadResponse;
import com.plateapp.plate_main.feed.service.ImageFeedUploadService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PathVariable;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ImageFeedUploadController {

    private final ImageFeedUploadService imageFeedUploadService;

    @PostMapping("/image-feeds")
    public ResponseEntity<ImageFeedUploadResponse> createImageFeed(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam("content") String content,
            @RequestParam("address") String address,
            @RequestParam(value = "storeName", required = false) String storeName,
            @RequestParam(value = "placeId", required = false) String placeId,
            @RequestParam(value = "withFriends", required = false) String withFriends,
            @RequestParam(value = "openYn", required = false) String openYn,
            @RequestParam(value = "useYn", required = false) String useYn
    ) {
        String username = currentUsername();
        ImageFeedUploadResponse response = imageFeedUploadService.createFeed(
                files,
                content,
                address,
                storeName,
                placeId,
                withFriends,
                openYn,
                useYn,
                username
        );
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/image-feeds/{feedId}")
    public ResponseEntity<?> updateImageFeed(
            @PathVariable("feedId") Integer feedId,
            @RequestBody ImageFeedUpdateRequest body
    ) {
        if (body.content == null || body.content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        if (body.address == null || body.address.isBlank()) {
            throw new IllegalArgumentException("address is required");
        }
        String username = currentUsername();
        imageFeedUploadService.updateFeed(
                feedId,
                body.content,
                body.address,
                body.storeName,
                body.placeId,
                body.useYn,
                username
        );
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    @PostMapping("/image-feeds/{feedId}/images")
    public ResponseEntity<ImageFeedUploadResponse> addImageFeedImages(
            @PathVariable("feedId") Integer feedId,
            @RequestParam("files") List<MultipartFile> files
    ) {
        String username = currentUsername();
        ImageFeedUploadResponse response = imageFeedUploadService.addImages(feedId, files, username);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/image-feeds/{feedId}")
    public ResponseEntity<?> deleteImageFeed(@PathVariable("feedId") Integer feedId) {
        String username = currentUsername();
        imageFeedUploadService.deleteFeed(feedId, username);
        return ResponseEntity.ok(java.util.Map.of("ok", true));
    }

    private String currentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated()) {
            String name = auth.getName();
            if (name != null && !name.isBlank() && !"anonymousUser".equalsIgnoreCase(name)) {
                return name;
            }
        }
        throw new IllegalStateException("Unauthenticated");
    }

    public static class ImageFeedUpdateRequest {
        public String content;
        public String address;
        public String storeName;
        public String placeId;
        public String withFriends;
        public String openYn;
        public String useYn;
    }
}
