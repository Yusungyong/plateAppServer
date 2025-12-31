// src/main/java/com/plateapp/plate_main/feed/dto/ImageFeedContextResponse.java
package com.plateapp.plate_main.feed.dto;

import java.util.List;

public class ImageFeedContextResponse {

  private final List<Integer> feedIds;
  private final int initialIndex;

  public ImageFeedContextResponse(List<Integer> feedIds, int initialIndex) {
    this.feedIds = feedIds;
    this.initialIndex = initialIndex;
  }

  public List<Integer> getFeedIds() {
    return feedIds;
  }

  public int getInitialIndex() {
    return initialIndex;
  }
}
