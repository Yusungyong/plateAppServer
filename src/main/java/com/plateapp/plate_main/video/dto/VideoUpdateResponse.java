package com.plateapp.plate_main.video.dto;

import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VideoUpdateResponse {
    Integer storeId;
    String fileName;
    String thumbnail;
    Integer videoDuration;
    Long videoSize;
    LocalDate updatedAt;
}
