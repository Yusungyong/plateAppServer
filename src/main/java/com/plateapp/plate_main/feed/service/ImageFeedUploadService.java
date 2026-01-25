package com.plateapp.plate_main.feed.service;

import com.plateapp.plate_main.common.image.ImageProcessingService;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.feed.dto.ImageFeedUploadResponse;
import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageFeedUploadService {

    private final ImageFeedRepository imageFeedRepository;
    private final S3UploadService s3UploadService;
    private final ImageProcessingService imageProcessingService;

    @Transactional
    public ImageFeedUploadResponse createFeed(
            List<MultipartFile> files,
            String content,
            String address,
            String storeName,
            String placeId,
            String withFriendsRaw,
            String openYn,
            String useYn,
            String username
    ) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files is required");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("content is required");
        }
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("address is required");
        }

        String safeUseYn = (useYn == null || useYn.isBlank()) ? "Y" : useYn;
        String safeOpenYn = (openYn == null || openYn.isBlank()) ? "Y" : openYn;

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalName = safeFileName(file.getOriginalFilename(), "image.jpg");
            byte[] rawBytes;
            try {
                rawBytes = file.getBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read image bytes", e);
            }

            byte[] optimized;
            try {
                optimized = imageProcessingService.resizeMax(rawBytes, 1280, 1280, "jpg");
            } catch (IOException e) {
                throw new IllegalStateException("Failed to process image", e);
            }

            String uploadedUrl = s3UploadService.uploadBytesWithPrefix(
                    s3UploadService.getImagePrefix(),
                    originalName,
                    optimized,
                    "image/jpeg"
            );
            imageUrls.add(uploadedUrl);
        }

        if (imageUrls.isEmpty()) {
            throw new IllegalArgumentException("files is required");
        }

        LocalDateTime now = LocalDateTime.now();
        Fp400ImageFeed feed = new Fp400ImageFeed();
        feed.setUsername(username);
        feed.setContent(content);
        feed.setImages(String.join(",", imageUrls));
        feed.setCreatedAt(now);
        feed.setUpdatedAt(now);
        feed.setLocation(address);
        feed.setStoreName(storeName);
        feed.setPlaceId(placeId);
        feed.setUseYn(safeUseYn);
        feed.setThumbnail(imageUrls.get(0));

        Fp400ImageFeed saved = imageFeedRepository.save(feed);

        ImageFeedUploadResponse response = new ImageFeedUploadResponse();
        response.feedId = saved.getFeedId();
        response.content = saved.getContent();
        response.storeName = saved.getStoreName();
        response.placeId = saved.getPlaceId();
        response.address = saved.getLocation();
        response.useYn = saved.getUseYn();
        response.openYn = safeOpenYn;
        response.createdAt = OffsetDateTime.of(now, ZoneOffset.UTC);
        response.withFriends = parseWithFriends(withFriendsRaw);
        response.images = buildImageItems(imageUrls);
        return response;
    }

    @Transactional
    public void updateFeed(
            Integer feedId,
            String content,
            String address,
            String storeName,
            String placeId,
            String useYn,
            String username
    ) {
        Fp400ImageFeed feed = imageFeedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("feed not found"));
        if (!username.equals(feed.getUsername())) {
            throw new AccessDeniedException("feed owner mismatch");
        }

        if (content != null && !content.isBlank()) {
            feed.setContent(content);
        }
        if (address != null && !address.isBlank()) {
            feed.setLocation(address);
        }
        if (storeName != null) {
            feed.setStoreName(storeName);
        }
        if (placeId != null) {
            feed.setPlaceId(placeId);
        }
        if (useYn != null && !useYn.isBlank()) {
            feed.setUseYn(useYn);
        }
        feed.setUpdatedAt(LocalDateTime.now());
        imageFeedRepository.save(feed);
    }

    @Transactional
    public ImageFeedUploadResponse addImages(
            Integer feedId,
            List<MultipartFile> files,
            String username
    ) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files is required");
        }
        Fp400ImageFeed feed = imageFeedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("feed not found"));
        if (!username.equals(feed.getUsername())) {
            throw new AccessDeniedException("feed owner mismatch");
        }

        List<String> newUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            String originalName = safeFileName(file.getOriginalFilename(), "image.jpg");
            byte[] rawBytes;
            try {
                rawBytes = file.getBytes();
            } catch (IOException e) {
                throw new IllegalStateException("Failed to read image bytes", e);
            }

            byte[] optimized;
            try {
                optimized = imageProcessingService.resizeMax(rawBytes, 1280, 1280, "jpg");
            } catch (IOException e) {
                throw new IllegalStateException("Failed to process image", e);
            }

            String uploadedUrl = s3UploadService.uploadBytesWithPrefix(
                    s3UploadService.getImagePrefix(),
                    originalName,
                    optimized,
                    "image/jpeg"
            );
            newUrls.add(uploadedUrl);
        }

        if (newUrls.isEmpty()) {
            throw new IllegalArgumentException("files is required");
        }

        List<String> currentUrls = parseImages(feed.getImages());
        currentUrls.addAll(newUrls);
        feed.setImages(String.join(",", currentUrls));
        if (feed.getThumbnail() == null || feed.getThumbnail().isBlank()) {
            feed.setThumbnail(currentUrls.get(0));
        }
        feed.setUpdatedAt(LocalDateTime.now());
        imageFeedRepository.save(feed);

        ImageFeedUploadResponse response = new ImageFeedUploadResponse();
        response.feedId = feed.getFeedId();
        response.images = buildImageItems(currentUrls);
        response.content = feed.getContent();
        response.storeName = feed.getStoreName();
        response.placeId = feed.getPlaceId();
        response.address = feed.getLocation();
        response.useYn = feed.getUseYn();
        response.openYn = "Y";
        response.createdAt = OffsetDateTime.of(feed.getCreatedAt(), ZoneOffset.UTC);
        response.withFriends = List.of();
        return response;
    }

    @Transactional
    public void deleteFeed(Integer feedId, String username) {
        Fp400ImageFeed feed = imageFeedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("feed not found"));
        if (!username.equals(feed.getUsername())) {
            throw new AccessDeniedException("feed owner mismatch");
        }

        for (String url : parseImages(feed.getImages())) {
            s3UploadService.deleteObjectByUrl(url);
        }
        s3UploadService.deleteObjectByUrl(feed.getThumbnail());

        feed.setUseYn("N");
        feed.setUpdatedAt(LocalDateTime.now());
        imageFeedRepository.save(feed);
    }

    private List<ImageFeedUploadResponse.ImageItem> buildImageItems(List<String> urls) {
        List<ImageFeedUploadResponse.ImageItem> items = new ArrayList<>();
        int order = 1;
        for (String url : urls) {
            items.add(new ImageFeedUploadResponse.ImageItem(order++, url));
        }
        return items;
    }

    private List<String> parseWithFriends(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        return List.of(trimmed.split(","))
                .stream()
                .map(String::trim)
                .map(name -> name.startsWith("@") ? name.substring(1) : name)
                .filter(name -> !name.isBlank())
                .map(name -> name.toLowerCase(Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }

    private List<String> parseImages(String images) {
        if (images == null || images.isBlank()) {
            return new ArrayList<>();
        }
        return new ArrayList<>(Arrays.stream(images.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList());
    }

    private String safeFileName(String original, String fallback) {
        if (original == null || original.isBlank()) {
            return fallback;
        }
        return original;
    }
}
