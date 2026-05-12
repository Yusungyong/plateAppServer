package com.plateapp.plate_main.home.service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

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
import com.plateapp.plate_main.video.entity.Fp310Place;
import com.plateapp.plate_main.video.repository.Fp310PlaceRepository;

@Service
public class HomeImageThumbnailService {
  private static final int IMAGE_HOME_SIZE_MAX = 100;
  private static final int IMAGE_HOME_FETCH_MAX = 180;
  private static final int IMAGE_HOME_FETCH_MULTIPLIER = 3;
  private static final double NEARBY_RADIUS_DEFAULT = 1500.0;
  private static final double NEARBY_RADIUS_MIN = 300.0;
  private static final double NEARBY_RADIUS_MAX = 5000.0;
  private static final double EARTH_RADIUS_METERS = 6_371_000.0;
  private static final String FLAG_Y = "Y";

  private final Fp400FeedRepository feedRepository;
  private final BlockRepository blockRepository;
  private final ReportRepository reportRepository;
  private final Fp310PlaceRepository fp310PlaceRepository;

  public HomeImageThumbnailService(
      Fp400FeedRepository feedRepository,
      BlockRepository blockRepository,
      ReportRepository reportRepository,
      Fp310PlaceRepository fp310PlaceRepository
  ) {
    this.feedRepository = feedRepository;
    this.blockRepository = blockRepository;
    this.reportRepository = reportRepository;
    this.fp310PlaceRepository = fp310PlaceRepository;
  }

  @Transactional(readOnly = true)
  public HomeImageThumbnailResponse getLatestThumbs(
      int size,
      String sortType,
      Double lat,
      Double lng,
      Double radius,
      String username,
      String groupId
  ) {
    if (size < 1 || size > IMAGE_HOME_SIZE_MAX) {
      throw new AppException(ErrorCode.COMMON_INVALID_INPUT);
    }

    Set<String> excluded = loadExcludedUsernames(username);
    GroupKey groupKey = parseGroupId(groupId);
    int candidateSize = resolveCandidateFetchSize(size);
    List<Fp400Feed> feeds;
    Double safeRadius = null;

    if ("NEARBY".equalsIgnoreCase(sortType)) {
      if (lat == null || lng == null) {
        throw new AppException(ErrorCode.COMMON_MISSING_PARAMETER);
      }
      if (groupKey.storeName != null && groupKey.placeId == null) {
        return new HomeImageThumbnailResponse(List.of());
      }
      safeRadius = normalizeRadius(radius);
      feeds = feedRepository.findNearbyForHomeByGroup(
          lat,
          lng,
          safeRadius,
          groupKey.placeId,
          groupKey.storeName,
          PageRequest.of(0, candidateSize)
      );
    } else {
      feeds = feedRepository.findLatestForHomeByGroup(
          groupKey.placeId,
          groupKey.storeName,
          PageRequest.of(0, candidateSize)
      );
    }

    List<Fp400Feed> filteredFeeds = feeds.stream()
        .filter(feed -> excluded.isEmpty() || feed.getUsername() == null || !excluded.contains(feed.getUsername()))
        .toList();

    return new HomeImageThumbnailResponse(rerankFeeds(filteredFeeds, size, lat, lng, safeRadius));
  }

  private int resolveCandidateFetchSize(int size) {
    return Math.min(Math.max(size * IMAGE_HOME_FETCH_MULTIPLIER, size), IMAGE_HOME_FETCH_MAX);
  }

  private double normalizeRadius(Double radius) {
    if (radius == null) {
      return NEARBY_RADIUS_DEFAULT;
    }
    if (radius < NEARBY_RADIUS_MIN) {
      return NEARBY_RADIUS_MIN;
    }
    if (radius > NEARBY_RADIUS_MAX) {
      return NEARBY_RADIUS_MAX;
    }
    return radius;
  }

  private List<HomeImageThumbnailItem> rerankFeeds(
      List<Fp400Feed> feeds,
      int size,
      Double lat,
      Double lng,
      Double radiusMeters
  ) {
    if (feeds.isEmpty()) {
      return List.of();
    }

    Map<String, Fp310Place> placeMap = loadPlaceMap(feeds);
    List<ScoredImageCandidate> scored = feeds.stream()
        .map(feed -> new ScoredImageCandidate(feed, calculateScore(feed, placeMap, lat, lng, radiusMeters)))
        .sorted(Comparator.comparingDouble(ScoredImageCandidate::baseScore).reversed())
        .toList();

    return diversify(scored).stream()
        .limit(size)
        .map(candidate -> toItem(candidate.feed()))
        .toList();
  }

  private double calculateScore(
      Fp400Feed feed,
      Map<String, Fp310Place> placeMap,
      Double lat,
      Double lng,
      Double radiusMeters
  ) {
    double score = recencyScore(feed);
    score += Math.min(imageCount(feed) * 0.4, 2.0);

    if (feed.getFeedTitle() != null && !feed.getFeedTitle().isBlank()) {
      score += 0.25;
    }
    if (feed.getStoreName() != null && !feed.getStoreName().isBlank()) {
      score += 0.2;
    }
    if (feed.getPlaceId() != null && !feed.getPlaceId().isBlank()) {
      score += 0.15;
    }

    Double distanceMeters = resolveDistanceMeters(feed, placeMap, lat, lng);
    if (distanceMeters != null) {
      double effectiveRadius = radiusMeters != null ? radiusMeters : NEARBY_RADIUS_DEFAULT;
      double normalized = Math.min(distanceMeters / effectiveRadius, 1.0);
      score += (1.0 - normalized) * 3.0;
    }

    return score;
  }

  private double recencyScore(Fp400Feed feed) {
    LocalDateTime referenceTime = feed.getUpdatedAt() != null ? feed.getUpdatedAt() : feed.getCreatedAt();
    if (referenceTime == null) {
      return 0.0;
    }

    long hoursOld = Math.max(0, ChronoUnit.HOURS.between(referenceTime, LocalDateTime.now()));
    double decay = Math.min(hoursOld, 120) * 0.04;
    return Math.max(0.5, 5.0 - decay);
  }

  private int imageCount(Fp400Feed feed) {
    String images = feed.getImages();
    if (images == null || images.isBlank()) {
      return 0;
    }
    return (int) java.util.Arrays.stream(images.split(","))
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .count();
  }

  private Map<String, Fp310Place> loadPlaceMap(List<Fp400Feed> feeds) {
    List<String> placeIds = feeds.stream()
        .map(Fp400Feed::getPlaceId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    if (placeIds.isEmpty()) {
      return Map.of();
    }

    List<Fp310Place> places = fp310PlaceRepository.findByPlaceIdInAndUseYnAndDeletedAtIsNull(placeIds, FLAG_Y);
    if (places == null || places.isEmpty()) {
      return Map.of();
    }

    return places.stream()
        .filter(place -> place.getPlaceId() != null)
        .collect(Collectors.toMap(
            Fp310Place::getPlaceId,
            place -> place,
            (left, right) -> left,
            LinkedHashMap::new
        ));
  }

  private Double resolveDistanceMeters(
      Fp400Feed feed,
      Map<String, Fp310Place> placeMap,
      Double lat,
      Double lng
  ) {
    if (lat == null || lng == null || feed.getPlaceId() == null) {
      return null;
    }

    Fp310Place place = placeMap.get(feed.getPlaceId());
    if (place == null || place.getLatitude() == null || place.getLongitude() == null) {
      return null;
    }

    return calculateDistanceMeters(lat, lng, place.getLatitude(), place.getLongitude());
  }

  private double calculateDistanceMeters(double startLat, double startLng, double endLat, double endLng) {
    double latDistance = Math.toRadians(endLat - startLat);
    double lngDistance = Math.toRadians(endLng - startLng);
    double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
        + Math.cos(Math.toRadians(startLat))
        * Math.cos(Math.toRadians(endLat))
        * Math.sin(lngDistance / 2)
        * Math.sin(lngDistance / 2);
    double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return EARTH_RADIUS_METERS * c;
  }

  private List<ScoredImageCandidate> diversify(List<ScoredImageCandidate> candidates) {
    List<ScoredImageCandidate> remaining = new ArrayList<>(candidates);
    List<ScoredImageCandidate> diversified = new ArrayList<>(candidates.size());
    Map<String, Integer> usernameCounts = new LinkedHashMap<>();
    Map<String, Integer> placeCounts = new LinkedHashMap<>();

    while (!remaining.isEmpty()) {
      ScoredImageCandidate next = remaining.stream()
          .max(Comparator.comparingDouble(candidate -> adjustedScore(candidate, usernameCounts, placeCounts)))
          .orElse(remaining.get(0));

      diversified.add(next);
      remaining.remove(next);

      String username = next.feed().getUsername();
      if (username != null && !username.isBlank()) {
        usernameCounts.merge(username, 1, Integer::sum);
      }

      String placeId = next.feed().getPlaceId();
      if (placeId != null && !placeId.isBlank()) {
        placeCounts.merge(placeId, 1, Integer::sum);
      }
    }

    return diversified;
  }

  private double adjustedScore(
      ScoredImageCandidate candidate,
      Map<String, Integer> usernameCounts,
      Map<String, Integer> placeCounts
  ) {
    double adjusted = candidate.baseScore();

    String username = candidate.feed().getUsername();
    if (username != null && !username.isBlank()) {
      adjusted -= usernameCounts.getOrDefault(username, 0) * 1.0;
    }

    String placeId = candidate.feed().getPlaceId();
    if (placeId != null && !placeId.isBlank()) {
      adjusted -= placeCounts.getOrDefault(placeId, 0) * 0.8;
    }

    return adjusted;
  }

  private HomeImageThumbnailItem toItem(Fp400Feed feed) {
    String[] arr = feed.getImages() == null ? new String[0] : feed.getImages().split(",");
    String thumb = (arr.length > 0) ? arr[0] : null;
    int imageCount = (int) java.util.Arrays.stream(arr)
        .map(String::trim)
        .filter(value -> !value.isEmpty())
        .count();

    return new HomeImageThumbnailItem(
        feed.getFeedNo(),
        thumb,
        feed.getStoreName(),
        feed.getPlaceId(),
        buildGroupId(feed.getPlaceId(), feed.getStoreName()),
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

  private GroupKey parseGroupId(String groupId) {
    if (groupId == null || groupId.isBlank()) {
      return new GroupKey(null, null);
    }
    if (groupId.startsWith("place:")) {
      String placeId = groupId.substring("place:".length()).trim();
      if (placeId.isEmpty()) {
        throw new AppException(ErrorCode.COMMON_INVALID_INPUT);
      }
      return new GroupKey(placeId, null);
    }
    if (groupId.startsWith("store:")) {
      String storeName = groupId.substring("store:".length()).trim();
      if (storeName.isEmpty()) {
        throw new AppException(ErrorCode.COMMON_INVALID_INPUT);
      }
      return new GroupKey(null, storeName);
    }
    throw new AppException(ErrorCode.COMMON_INVALID_INPUT);
  }

  private String buildGroupId(String placeId, String storeName) {
    if (placeId != null && !placeId.isBlank()) {
      return "place:" + placeId;
    }
    if (storeName != null && !storeName.isBlank()) {
      return "store:" + storeName;
    }
    return null;
  }

  private record GroupKey(String placeId, String storeName) {}

  private record ScoredImageCandidate(Fp400Feed feed, double baseScore) {}
}
