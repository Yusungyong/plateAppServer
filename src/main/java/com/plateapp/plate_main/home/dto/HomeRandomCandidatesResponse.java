package com.plateapp.plate_main.home.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class HomeRandomCandidatesResponse {
    public List<Item> items;

    public HomeRandomCandidatesResponse(List<Item> items) {
        this.items = items;
    }

    public static class Item {
        public String type;
        public Integer storeId;
        public Integer feedNo;
        public String placeId;
        public String storeName;
        public String address;
        public String thumbnail;
        public OffsetDateTime createdAt;

        public Item(
                String type,
                Integer storeId,
                Integer feedNo,
                String placeId,
                String storeName,
                String address,
                String thumbnail,
                OffsetDateTime createdAt
        ) {
            this.type = type;
            this.storeId = storeId;
            this.feedNo = feedNo;
            this.placeId = placeId;
            this.storeName = storeName;
            this.address = address;
            this.thumbnail = thumbnail;
            this.createdAt = createdAt;
        }
    }
}
