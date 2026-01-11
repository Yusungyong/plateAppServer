package com.plateapp.plate_main.profile.controller;

import com.plateapp.plate_main.profile.dto.MyProfileRequest;
import com.plateapp.plate_main.profile.dto.MyProfileResponse;
import com.plateapp.plate_main.profile.service.MyProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/my")
public class MyProfileController {

    private final MyProfileService myProfileService;

    @PostMapping("/profile")
    public ResponseEntity<MyProfileResponse> getProfile(@RequestBody @Valid MyProfileRequest request) {
        return ResponseEntity.ok(myProfileService.getProfile(request));
    }
}
