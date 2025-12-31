package com.plateapp.plate_main.video.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class HomeVideoThumbnailDTO {
    private Integer storeId;
    private String title;
    private String fileName;
    private String thumbnail;
    private Integer videoDuration;
    private String muteYn;
    private BigDecimal videoSize;
    private String storeName;
    private String address;
    private String placeId;
    private LocalDate updatedAt;
}
