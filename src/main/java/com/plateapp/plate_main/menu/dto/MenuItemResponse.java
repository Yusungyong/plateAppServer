package com.plateapp.plate_main.menu.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class MenuItemResponse {
    String itemId;
    Integer storeId;
    String itemName;
    String price;
    String description;
    String menuImage;
    String menuTitle;
    String placeId;
    String storeName;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    LocalDate deletedAt;
}
