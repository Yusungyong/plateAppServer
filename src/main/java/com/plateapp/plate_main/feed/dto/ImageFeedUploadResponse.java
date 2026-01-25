package com.plateapp.plate_main.feed.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class ImageFeedUploadResponse {
    public Integer feedId;
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
        public Integer orderNo;
        public String fileName;

        public ImageItem(Integer orderNo, String fileName) {
            this.orderNo = orderNo;
            this.fileName = fileName;
        }
    }
}
