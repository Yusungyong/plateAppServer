package com.plateapp.plate_main.home.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.plateapp.plate_main.block.repository.BlockRepository;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.feed.entity.Fp400Feed;
import com.plateapp.plate_main.feed.repository.Fp400FeedRepository;
import com.plateapp.plate_main.home.dto.HomeImageThumbnailItem;
import com.plateapp.plate_main.home.dto.HomeImageThumbnailResponse;
import com.plateapp.plate_main.report.repository.ReportRepository;

@Service
public class HomeImageThumbnailService {

  private final Fp400FeedRepository feedRepository;
  private final BlockRepository blockRepository;
  private final ReportRepository reportRepository;

  public HomeImageThumbnailService(
      Fp400FeedRepository feedRepository,
      BlockRepository blockRepository,
      ReportRepository reportRepository
  ) {
    this.feedRepository = feedRepository;
    this.blockRepository = blockRepository;
    this.reportRepository = reportRepository;
  }

  @Transactional(readOnly = true)
  public HomeImageThumbnailResponse getLatestThumbs(
      int size,
      String sortType,
      Double lat,
      Double lng,
      Double radius,
      String username
  ) {
    if (size < 1 || size > 100) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT);
    }

    Set<String> excluded = loadExcludedUsernames(username);
    List<Fp400Feed> feeds;
    if ("NEARBY".equalsIgnoreCase(sortType)) {
      if (lat == null || lng == null) {
        throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER);
      }
      double safeRadius = normalizeRadius(radius);
      feeds = feedRepository.findNearbyForHome(
          lat,
          lng,
          safeRadius,
          PageRequest.of(0, size)
      );
    } else {
      feeds = feedRepository.findLatestForHome(PageRequest.of(0, size));
    }

    List<HomeImageThumbnailItem> items = feeds.stream()
        .filter(feed -> excluded.isEmpty() || feed.getUsername() == null || !excluded.contains(feed.getUsername()))
        .map(this::toItem)
        .toList();

    return new HomeImageThumbnailResponse(items);
  }

  private double normalizeRadius(Double radius) {
    if (radius == null) {
      return 1500.0;
    }
    if (radius < 300.0) {
      return 300.0;
    }
    if (radius > 5000.0) {
      return 5000.0;
    }
    return radius;
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

  private Set<String> loadExcludedUsernames(String username) {
    if (username == null || username.isBlank()) {
      return Set.of();
    }
    Set<String> excluded = new HashSet<>();
    List<String> blocked = blockRepository.findBlockedUsernames(username);
    if (blocked != null) {
      excluded.addAll(blocked);
    }
    List<String> reported = reportRepository.findReportedUsernames(username);
    if (reported != null) {
      excluded.addAll(reported);
    }
    return excluded;
  }
}
