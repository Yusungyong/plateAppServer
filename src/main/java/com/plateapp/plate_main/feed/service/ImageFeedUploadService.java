package com.plateapp.plate_main.feed.service;

import com.plateapp.plate_main.common.image.ImageProcessingService;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.comment.repository.FeedCommentRepository;
import com.plateapp.plate_main.comment.repository.FeedReplyRepository;
import com.plateapp.plate_main.feed.dto.ImageFeedUploadResponse;
import com.plateapp.plate_main.feed.entity.Fp400ImageFeed;
import com.plateapp.plate_main.feed.repository.ImageFeedRepository;
import com.plateapp.plate_main.friend.repository.Fp200VisitRepository;
import com.plateapp.plate_main.like.repository.ImageFeedLikeRepository;
import com.plateapp.plate_main.menu.repository.Fp320MenuRepository;
import com.plateapp.plate_main.restaurant.repository.RestaurantRepository;
import com.plateapp.plate_main.video.service.PlaceService;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImageFeedUploadService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "heic", "heif");
    private static final Set<String> ALLOWED_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/heic",
            "image/heif"
    );

    private final ImageFeedRepository imageFeedRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final FeedReplyRepository feedReplyRepository;
    private final ImageFeedLikeRepository imageFeedLikeRepository;
    private final Fp200VisitRepository fp200VisitRepository;
    private final Fp320MenuRepository fp320MenuRepository;
    private final S3UploadService s3UploadService;
    private final ImageProcessingService imageProcessingService;
    private final PlaceService placeService;
    private final RestaurantRepository restaurantRepository;

    @Transactional
    public ImageFeedUploadResponse createFeed(
            List<MultipartFile> files,
            String content,
            String address,
            String storeName,
            String placeId,
            Long restaurantId,
            Double lat,
            Double lng,
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
            imageUrls.add(processAndUploadImage(file));
        }

        if (imageUrls.isEmpty()) {
            throw new IllegalArgumentException("files is required");
        }

        LocalDateTime now = LocalDateTime.now();
        Fp400ImageFeed feed = new Fp400ImageFeed();
        feed.setUsername(username);
        feed.setContent(content);
        feed.setImages(String.join(",", imageUrls));
        feed.setThumbnail(resolveFeedThumbnail(imageUrls));
        feed.setCreatedAt(now);
        feed.setUpdatedAt(now);
        feed.setLocation(address);
        feed.setStoreName(storeName);
        feed.setRestaurantId(resolveRestaurantId(restaurantId, storeName, address));
        feed.setPlaceId(placeId);
        feed.setUseYn(safeUseYn);

        Fp400ImageFeed saved = imageFeedRepository.save(feed);
        upsertPlace(placeId, address, lat, lng);

        ImageFeedUploadResponse response = new ImageFeedUploadResponse();
        response.feedId = saved.getFeedId();
        response.restaurantId = saved.getRestaurantId();
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
            Long restaurantId,
            Double lat,
            Double lng,
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
        feed.setRestaurantId(resolveRestaurantId(restaurantId, feed.getStoreName(), feed.getLocation()));
        feed.setUpdatedAt(LocalDateTime.now());
        imageFeedRepository.save(feed);
        upsertPlace(feed.getPlaceId(), feed.getLocation(), lat, lng);
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
            newUrls.add(processAndUploadImage(file));
        }

        if (newUrls.isEmpty()) {
            throw new IllegalArgumentException("files is required");
        }

        List<String> currentUrls = parseImages(feed.getImages());
        currentUrls.addAll(newUrls);
        feed.setImages(String.join(",", currentUrls));
        feed.setThumbnail(resolveFeedThumbnail(currentUrls));
        feed.setUpdatedAt(LocalDateTime.now());
        imageFeedRepository.save(feed);

        ImageFeedUploadResponse response = new ImageFeedUploadResponse();
        response.feedId = feed.getFeedId();
        response.restaurantId = feed.getRestaurantId();
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
    public ImageFeedUploadResponse.ImageItem replaceImage(
            Integer feedId,
            Integer imageId,
            MultipartFile file,
            String username
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (imageId == null || imageId <= 0) {
            throw new IllegalArgumentException("imageId is invalid");
        }

        Fp400ImageFeed feed = imageFeedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("feed not found"));
        if (!username.equals(feed.getUsername())) {
            throw new AccessDeniedException("feed owner mismatch");
        }

        List<String> currentUrls = parseImages(feed.getImages());
        int index = imageId - 1;
        if (index < 0 || index >= currentUrls.size()) {
            throw new IllegalArgumentException("image not found");
        }

        String oldRelativePath = currentUrls.get(index);
        String newRelativePath = processAndUploadImage(file);
        currentUrls.set(index, newRelativePath);

        feed.setImages(String.join(",", currentUrls));
        feed.setThumbnail(resolveFeedThumbnail(currentUrls));
        feed.setUpdatedAt(LocalDateTime.now());
        imageFeedRepository.save(feed);

        deleteImageObjects(oldRelativePath);

        return buildImageItem(imageId, newRelativePath);
    }

    @Transactional
    public void deleteImage(
            Integer feedId,
            Integer imageId,
            String username
    ) {
        if (imageId == null || imageId <= 0) {
            throw new IllegalArgumentException("imageId is invalid");
        }

        Fp400ImageFeed feed = imageFeedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("feed not found"));
        if (!username.equals(feed.getUsername())) {
            throw new AccessDeniedException("feed owner mismatch");
        }

        List<String> currentUrls = parseImages(feed.getImages());
        if (currentUrls.size() <= 1) {
            throw new IllegalArgumentException("last image cannot be deleted");
        }

        int index = imageId - 1;
        if (index < 0 || index >= currentUrls.size()) {
            throw new IllegalArgumentException("image not found");
        }

        String removedRelativePath = currentUrls.remove(index);
        feed.setImages(String.join(",", currentUrls));
        feed.setThumbnail(resolveFeedThumbnail(currentUrls));
        feed.setUpdatedAt(LocalDateTime.now());
        imageFeedRepository.save(feed);

        deleteImageObjects(removedRelativePath);
    }

    @Transactional
    public ImageFeedUploadResponse reorderImages(
            Integer feedId,
            List<Integer> imageIds,
            String username
    ) {
        if (imageIds == null || imageIds.isEmpty()) {
            throw new IllegalArgumentException("imageIds is required");
        }

        Fp400ImageFeed feed = imageFeedRepository.findById(feedId)
                .orElseThrow(() -> new IllegalArgumentException("feed not found"));
        if (!username.equals(feed.getUsername())) {
            throw new AccessDeniedException("feed owner mismatch");
        }

        List<String> currentUrls = parseImages(feed.getImages());
        if (currentUrls.size() != imageIds.size()) {
            throw new IllegalArgumentException("imageIds size mismatch");
        }

        boolean[] used = new boolean[currentUrls.size()];
        List<String> reorderedUrls = new ArrayList<>();
        for (Integer imageId : imageIds) {
            if (imageId == null || imageId <= 0 || imageId > currentUrls.size()) {
                throw new IllegalArgumentException("imageId is invalid");
            }
            int index = imageId - 1;
            if (used[index]) {
                throw new IllegalArgumentException("imageIds contains duplicates");
            }
            used[index] = true;
            reorderedUrls.add(currentUrls.get(index));
        }

        feed.setImages(String.join(",", reorderedUrls));
        feed.setThumbnail(resolveFeedThumbnail(reorderedUrls));
        feed.setUpdatedAt(LocalDateTime.now());
        imageFeedRepository.save(feed);

        ImageFeedUploadResponse response = new ImageFeedUploadResponse();
        response.feedId = feed.getFeedId();
        response.restaurantId = feed.getRestaurantId();
        response.images = buildImageItems(reorderedUrls);
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
            deleteImageObjects(url);
        }
        if (feed.getThumbnail() != null && !feed.getThumbnail().isBlank()) {
            deleteThumbnailObject(feed.getThumbnail());
        }

        List<Integer> commentIds = feedCommentRepository.findIdsByFeedId(feedId);
        if (!commentIds.isEmpty()) {
            feedReplyRepository.deleteByCommentIds(commentIds);
        }
        feedCommentRepository.hardDeleteByFeedId(feedId);
        imageFeedLikeRepository.deleteByFeedId(feedId);
        fp200VisitRepository.deleteByFeedId(feedId.longValue());
        fp320MenuRepository.deleteByFeedId(feedId);

        imageFeedRepository.delete(feed);
    }

    private List<ImageFeedUploadResponse.ImageItem> buildImageItems(List<String> urls) {
        List<ImageFeedUploadResponse.ImageItem> items = new ArrayList<>();
        int order = 1;
        for (String url : urls) {
            items.add(buildImageItem(order, url));
            order++;
        }
        return items;
    }

    private ImageFeedUploadResponse.ImageItem buildImageItem(int order, String url) {
        return new ImageFeedUploadResponse.ImageItem(
                order,
                order,
                url,
                resolveThumbnailUrl(buildThumbnailRelativePath(url))
        );
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

    private void upsertPlace(String placeId, String address, Double lat, Double lng) {
        if (placeId == null || placeId.isBlank() || address == null || address.isBlank()) {
            return;
        }
        PlaceService.PlaceRequest req = PlaceService.PlaceRequest.builder()
                .placeId(placeId)
                .address(address)
                .lat(lat)
                .lng(lng)
                .build();
        placeService.savePlace(req);
    }

    private Long resolveRestaurantId(Long requestedRestaurantId, String storeName, String address) {
        if (requestedRestaurantId != null) {
            if (!restaurantRepository.existsById(requestedRestaurantId)) {
                throw new IllegalArgumentException("restaurantId not found");
            }
            return requestedRestaurantId;
        }
        if (storeName == null || storeName.isBlank() || address == null || address.isBlank()) {
            return null;
        }
        List<Long> matches = restaurantRepository.findIdsByTitleAndAddress(storeName, address);
        return matches.size() == 1 ? matches.get(0) : null;
    }

    private String safeStoredImageFileName(String original, String fallback) {
        String safeName = (original == null || original.isBlank()) ? fallback : original;
        int dotIndex = safeName.lastIndexOf('.');
        String extension = dotIndex >= 0 ? safeName.substring(dotIndex + 1).trim().toLowerCase(Locale.ROOT) : "";
        String normalizedExtension = switch (extension) {
            case "jpg", "jpeg", "png", "webp", "heic", "heif" -> "jpg";
            default -> "jpg";
        };
        return UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT) + "." + normalizedExtension;
    }

    private String buildRelativePath(String filename) {
        String datePrefix = LocalDate.now().toString().replace("-", "");
        return datePrefix + "/" + filename;
    }

    private String buildThumbnailRelativePath(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }
        int slash = relativePath.indexOf('/');
        if (slash <= 0 || slash == relativePath.length() - 1) {
            return null;
        }
        String datePrefix = relativePath.substring(0, slash);
        String filename = relativePath.substring(slash + 1);
        return datePrefix + "/thumbnails/300x300/" + filename;
    }

    private String processAndUploadImage(MultipartFile file) {
        validateImageFile(file);
        String storedFileName = safeStoredImageFileName(file.getOriginalFilename(), "image.jpg");
        String relativePath = buildRelativePath(storedFileName);
        String thumbRelativePath = buildThumbnailRelativePath(relativePath);
        Path sourcePath = null;
        Path optimizedPath = null;
        Path thumbPath = null;
        try {
            sourcePath = Files.createTempFile("feed-upload-source-", ".bin");
            file.transferTo(sourcePath);
            optimizedPath = imageProcessingService.resizeMaxToTempFile(sourcePath, 1280, 1280, "jpg");
            thumbPath = createThumbnail(sourcePath, thumbRelativePath);
            uploadFile(optimizedPath, relativePath);
            uploadFile(thumbPath, thumbRelativePath);
            return relativePath;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to process image", e);
        } finally {
            deleteTempFile(sourcePath);
            deleteTempFile(optimizedPath);
            deleteTempFile(thumbPath);
        }
    }

    private String resolveFeedThumbnail(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            return null;
        }
        return buildThumbnailRelativePath(imageUrls.get(0));
    }

    private Path createThumbnail(Path sourcePath, String thumbRelativePath) throws IOException {
        if (sourcePath == null || thumbRelativePath == null) {
            return null;
        }
        return imageProcessingService.resizeCropCenterToTempFile(sourcePath, 300, 300, "jpg");
    }

    private void validateImageFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("image file is required");
        }
        if (file.getSize() > MAX_IMAGE_BYTES) {
            throw new IllegalArgumentException("image file must be 10MB or less");
        }

        String extension = extension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("unsupported image extension");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !ALLOWED_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("unsupported image content type");
        }
    }

    private String extension(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private void uploadFile(Path filePath, String relativePath) throws IOException {
        if (filePath == null || relativePath == null) {
            return;
        }
        try (InputStream inputStream = Files.newInputStream(filePath)) {
            s3UploadService.uploadStreamWithPrefixAndPath(
                    s3UploadService.getFeedImagePrefix(),
                    relativePath,
                    inputStream,
                    Files.size(filePath),
                    "image/jpeg"
            );
        }
    }

    private void deleteTempFile(Path path) {
        if (path == null) {
            return;
        }
        try {
            Files.deleteIfExists(path);
        } catch (IOException ignored) {
            // Temp file cleanup failure should not mask the upload result.
        }
    }

    private void deleteImageObjects(String relativePath) {
        if (relativePath == null || relativePath.isBlank()) {
            return;
        }
        String baseKey = s3UploadService.getFeedImagePrefix() + trimLeadingSlash(relativePath);
        s3UploadService.deleteObjectByKey(baseKey);
        String thumbRelativePath = buildThumbnailRelativePath(relativePath);
        if (thumbRelativePath != null) {
            deleteThumbnailObject(thumbRelativePath);
        }
    }

    private void deleteThumbnailObject(String thumbnailPath) {
        if (thumbnailPath == null || thumbnailPath.isBlank()) {
            return;
        }
        if (thumbnailPath.contains("://")) {
            s3UploadService.deleteObjectByUrl(thumbnailPath);
            return;
        }
        String thumbKey = s3UploadService.getFeedImagePrefix() + trimLeadingSlash(thumbnailPath);
        s3UploadService.deleteObjectByKey(thumbKey);
    }

    private String resolveThumbnailUrl(String thumbnailPath) {
        if (thumbnailPath == null || thumbnailPath.isBlank()) {
            return null;
        }
        return s3UploadService.toFeedImageUrl(thumbnailPath);
    }

    private String trimLeadingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.startsWith("/") ? value.substring(1) : value;
    }
}
