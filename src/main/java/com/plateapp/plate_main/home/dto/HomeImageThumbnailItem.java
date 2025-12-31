package com.plateapp.plate_main.home.dto;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HomeImageThumbnailItem {
  private Integer feedNo;
  private String thumbFileName;
  private String storeName;
  private String placeId;
  private Integer imageCount;
  private LocalDateTime createdAt;
}
