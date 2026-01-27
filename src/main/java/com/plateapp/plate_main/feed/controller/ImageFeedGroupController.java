package com.plateapp.plate_main.feed.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.feed.dto.ImageFeedGroupImagesResponse;
import com.plateapp.plate_main.feed.dto.ImageFeedGroupResponse;
import com.plateapp.plate_main.feed.service.ImageFeedGroupService;

@RestController
@RequestMapping("/api/image-feeds/groups")
public class ImageFeedGroupController {

    private final ImageFeedGroupService groupService;

    public ImageFeedGroupController(ImageFeedGroupService groupService) {
        this.groupService = groupService;
    }

    @GetMapping
    public ApiResponse<ImageFeedGroupResponse> listGroups(
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "lat", required = false) Double lat,
            @RequestParam(value = "lng", required = false) Double lng,
            @RequestParam(value = "radius", required = false) Integer radius
    ) {
        return ApiResponse.ok(groupService.getGroups(limit, cursor, sort, lat, lng, radius));
    }

    @GetMapping("/{groupId}/images")
    public ApiResponse<ImageFeedGroupImagesResponse> listGroupImages(
            @PathVariable("groupId") String groupId,
            @RequestParam(value = "limit", required = false) Integer limit,
            @RequestParam(value = "cursor", required = false) String cursor
    ) {
        return ApiResponse.ok(groupService.getGroupImages(groupId, limit, cursor));
    }
}
