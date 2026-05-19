package com.plateapp.plate_main.video.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoFeedItemDTO {

    private Integer storeId;
    private String placeId;

    private String title;
    private String storeName;
    private String address;
    private Double lat;
    private Double lng;

    private String fileName;
    private String thumbnail;

    private Integer videoDuration;
    private LocalDate createdAt;

    private Long commentCount;
    private String profileImageUrl;
    private String username;

    private Long likeCount;
    private Boolean likedByMe;
}
