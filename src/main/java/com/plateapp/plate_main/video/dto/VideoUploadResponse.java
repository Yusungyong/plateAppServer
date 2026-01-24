package com.plateapp.plate_main.video.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class VideoUploadResponse {
    Integer storeId;
    String fileName;
    String thumbnail;
    Integer videoDuration;
    Long videoSize;
}
