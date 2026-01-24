package com.plateapp.plate_main.video.controller;

import com.plateapp.plate_main.video.dto.VideoUploadResponse;
import com.plateapp.plate_main.video.service.VideoUploadService;
import com.plateapp.plate_main.video.service.PlaceService;
import com.plateapp.plate_main.friend.service.FriendVisitCommandService;
import com.plateapp.plate_main.friend.dto.FriendVisitCreateRequest;
import com.plateapp.plate_main.friend.dto.FriendVisitCreateResponse;
import com.plateapp.plate_main.friend.dto.FriendVisitDeleteResponse;
import com.plateapp.plate_main.friend.dto.FriendVisitUpdateRequest;
import com.plateapp.plate_main.friend.dto.FriendVisitUpdateResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VideoController {

    private final VideoUploadService videoUploadService;
    private final PlaceService placeService;
    private final FriendVisitCommandService friendVisitCommandService;

    @PostMapping("/videos")
    public ResponseEntity<VideoUploadResponse> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "thumbnail", required = false) MultipartFile thumbnail,
            @RequestParam("storeName") String storeName,
            @RequestParam("placeId") String placeId,
            @RequestParam("address") String address,
            @RequestParam(value = "muteYn", defaultValue = "N") String muteYn,
            @RequestParam(value = "openYn", defaultValue = "Y") String openYn,
            @RequestParam(value = "useYn", defaultValue = "Y") String useYn
    ) {
        String username = currentUsername();
        VideoUploadResponse res = videoUploadService.uploadVideo(file, thumbnail, username, storeName, placeId, address, muteYn, openYn, useYn);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/places")
    public ResponseEntity<?> savePlace(@RequestBody @Valid PlaceService.PlaceRequest req) {
        placeService.savePlace(req);
        return ResponseEntity.ok().body(java.util.Map.of("ok", true, "placeId", req.getPlaceId()));
    }

    @PostMapping("/friends/visits")
    public ResponseEntity<FriendVisitCreateResponse> saveFriendVisits(@RequestBody @Valid FriendVisitCreateRequest req) {
        String username = currentUsername();
        FriendVisitCreateResponse response = friendVisitCommandService.saveVisits(username, req);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/friends/visits/{id}")
    public ResponseEntity<FriendVisitUpdateResponse> updateFriendVisit(
            @PathVariable Integer id,
            @RequestBody @Valid FriendVisitUpdateRequest req
    ) {
        String username = currentUsername();
        FriendVisitUpdateResponse response = friendVisitCommandService.updateVisit(username, id, req);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/friends/visits/{id}")
    public ResponseEntity<FriendVisitDeleteResponse> deleteFriendVisit(@PathVariable Integer id) {
        String username = currentUsername();
        FriendVisitDeleteResponse response = friendVisitCommandService.deleteVisit(username, id);
        return ResponseEntity.ok(response);
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
}
