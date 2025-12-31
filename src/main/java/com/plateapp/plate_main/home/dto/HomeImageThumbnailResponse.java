package com.plateapp.plate_main.home.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HomeImageThumbnailResponse {
  private List<HomeImageThumbnailItem> items;
}
