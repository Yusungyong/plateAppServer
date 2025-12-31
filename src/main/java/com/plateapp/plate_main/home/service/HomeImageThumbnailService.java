package com.plateapp.plate_main.home.service;

import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.feed.entity.Fp400Feed;
import com.plateapp.plate_main.feed.repository.Fp400FeedRepository;
import com.plateapp.plate_main.home.dto.HomeImageThumbnailItem;
import com.plateapp.plate_main.home.dto.HomeImageThumbnailResponse;

@Service
public class HomeImageThumbnailService {

  private final Fp400FeedRepository feedRepository;

  public HomeImageThumbnailService(Fp400FeedRepository feedRepository) {
    this.feedRepository = feedRepository;
  }

  @Transactional(readOnly = true)
  public HomeImageThumbnailResponse getLatestThumbs(int size) {
    if (size < 1 || size > 20) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT);
    }

    List<Fp400Feed> feeds = feedRepository.findLatestForHome(PageRequest.of(0, size));

    List<HomeImageThumbnailItem> items = feeds.stream()
        .map(this::toItem)
        .toList();

    return new HomeImageThumbnailResponse(items);
  }

  private HomeImageThumbnailItem toItem(Fp400Feed feed) {
    // images는 "a.jpg,b.jpg,c.jpg" 형태, 이상 케이스 없다고 했으니 심플하게
    String images = feed.getImages();
    String[] arr = images.split(",");

    String thumb = (arr.length > 0) ? arr[0] : null;
    int imageCount = arr.length;

    return new HomeImageThumbnailItem(
        feed.getFeedNo(),
        thumb,
        feed.getStoreName(),
        feed.getPlaceId(),
        imageCount,
        feed.getCreatedAt()
    );
  }
}
