package com.plateapp.plate_main.feed.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class ImageFeedUploadResponse {
    public Integer feedId;
    public Long restaurantId;
    public String content;
    public String storeName;
    public String placeId;
    public String address;
    public String openYn;
    public String useYn;
    public OffsetDateTime createdAt;
    public List<String> withFriends;
    public List<ImageItem> images;

    public static class ImageItem {
        public Integer imageId;
        public Integer orderNo;
        public String fileName;
        public String thumbnailUrl;

        public ImageItem(Integer imageId, Integer orderNo, String fileName, String thumbnailUrl) {
            this.imageId = imageId;
            this.orderNo = orderNo;
            this.fileName = fileName;
            this.thumbnailUrl = thumbnailUrl;
        }
    }
}
