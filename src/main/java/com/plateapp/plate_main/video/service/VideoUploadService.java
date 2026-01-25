package com.plateapp.plate_main.video.service;

import com.plateapp.plate_main.common.image.ImageProcessingService;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.common.video.FfmpegService;
import com.plateapp.plate_main.video.dto.VideoUploadResponse;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.plateapp.plate_main.video.service.PlaceService;
import com.plateapp.plate_main.video.dto.VideoUpdateResponse;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoUploadService {

    private static final long MAX_FILE_BYTES = 100L * 1024 * 1024;
    private static final int MAX_DURATION_SECONDS = 120;
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("mp4", "mov");
    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of("video/mp4", "video/quicktime");

    private final S3UploadService s3UploadService;
    private final Fp300StoreRepository fp300StoreRepository;
    private final ImageProcessingService imageProcessingService;
    private final FfmpegService ffmpegService;
    private final PlaceService placeService;

    @Transactional
    public VideoUploadResponse uploadVideo(
            MultipartFile file,
            MultipartFile thumbnailFile,
            String username,
            String storeName,
            String placeId,
            String address,
            String muteYn,
            String openYn,
            String useYn
    ) {
        String safeOpenYn = (openYn == null || openYn.isBlank()) ? "Y" : openYn;
        String safeUseYn = (useYn == null || useYn.isBlank()) ? "Y" : useYn;
        String safeMuteYn = (muteYn == null || muteYn.isBlank()) ? "N" : muteYn;

        VideoMediaResult media = processVideoUpload(file, thumbnailFile);

        Fp300Store store = new Fp300Store();
        Long newId = fp300StoreRepository.nextStoreIdFallback();
        if (newId != null) {
            store.setStoreId(newId.intValue());
        }
        store.setTitle(storeName);
        store.setFileName(media.fileUrl);
        store.setAddress(address);
        store.setUsername(username);
        store.setCreatedAt(LocalDate.now());
        store.setUpdatedAt(LocalDate.now());
        store.setThumbnail(media.thumbnailUrl);
        store.setStoreName(storeName);
        store.setOpenYn(safeOpenYn);
        store.setUseYn(safeUseYn);
        store.setPlaceId(placeId);
        store.setMuteYn(safeMuteYn);
        store.setVideoDuration(media.durationSeconds);
        store.setVideoSize(java.math.BigDecimal.valueOf(media.size));

        Fp300Store saved = fp300StoreRepository.save(store);

        return VideoUploadResponse.builder()
                .storeId(saved.getStoreId())
                .fileName(saved.getFileName())
                .thumbnail(saved.getThumbnail())
                .videoDuration(saved.getVideoDuration())
                .videoSize(saved.getVideoSize() != null ? saved.getVideoSize().longValue() : null)
                .build();
    }

    @Transactional
    public VideoUpdateResponse updateVideoWithFile(
            Integer storeId,
            MultipartFile file,
            String username,
            String storeName,
            String placeId,
            String address,
            Double lat,
            Double lng,
            String muteYn,
            String openYn,
            String useYn
    ) {
        Fp300Store store = findOwnedStore(storeId, username);
        validateRequiredFields(storeName, placeId, address);

        String nextOpenYn = normalizeYn(openYn, store.getOpenYn());
        String nextUseYn = normalizeYn(useYn, store.getUseYn());
        String nextMuteYn = normalizeYn(muteYn, store.getMuteYn());

        VideoMediaResult media = processVideoUpload(file, null);
        String oldFile = store.getFileName();
        String oldThumb = store.getThumbnail();

        store.setTitle(storeName);
        store.setStoreName(storeName);
        store.setPlaceId(placeId);
        store.setAddress(address);
        store.setOpenYn(nextOpenYn);
        store.setUseYn(nextUseYn);
        store.setMuteYn(nextMuteYn);
        store.setFileName(media.fileUrl);
        store.setThumbnail(media.thumbnailUrl);
        store.setVideoDuration(media.durationSeconds);
        store.setVideoSize(java.math.BigDecimal.valueOf(media.size));
        store.setUpdatedAt(LocalDate.now());

        fp300StoreRepository.save(store);
        s3UploadService.deleteObjectByUrl(oldFile);
        s3UploadService.deleteObjectByUrl(oldThumb);
        upsertPlace(placeId, address, lat, lng);

        return VideoUpdateResponse.builder()
                .storeId(store.getStoreId())
                .fileName(store.getFileName())
                .thumbnail(store.getThumbnail())
                .videoDuration(store.getVideoDuration())
                .videoSize(store.getVideoSize() != null ? store.getVideoSize().longValue() : null)
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    @Transactional
    public void updateVideoMetadata(
            Integer storeId,
            String username,
            String storeName,
            String placeId,
            String address,
            Double lat,
            Double lng,
            String muteYn,
            String openYn,
            String useYn
    ) {
        Fp300Store store = findOwnedStore(storeId, username);
        validateRequiredFields(storeName, placeId, address);

        store.setTitle(storeName);
        store.setStoreName(storeName);
        store.setPlaceId(placeId);
        store.setAddress(address);
        store.setOpenYn(normalizeYn(openYn, store.getOpenYn()));
        store.setUseYn(normalizeYn(useYn, store.getUseYn()));
        store.setMuteYn(normalizeYn(muteYn, store.getMuteYn()));
        store.setUpdatedAt(LocalDate.now());

        fp300StoreRepository.save(store);
        upsertPlace(placeId, address, lat, lng);
    }

    @Transactional
    public void deleteVideo(Integer storeId, String username) {
        Fp300Store store = findOwnedStore(storeId, username);

        s3UploadService.deleteObjectByUrl(store.getFileName());
        s3UploadService.deleteObjectByUrl(store.getThumbnail());

        store.setUseYn("N");
        store.setDeletedAt(LocalDate.now());
        store.setUpdatedAt(LocalDate.now());
        fp300StoreRepository.save(store);
    }

    private Fp300Store findOwnedStore(Integer storeId, String username) {
        Fp300Store store = fp300StoreRepository.findById(storeId)
                .orElseThrow(() -> new IllegalArgumentException("video not found"));
        if (!username.equals(store.getUsername())) {
            throw new AccessDeniedException("video owner mismatch");
        }
        return store;
    }

    private void validateRequiredFields(String storeName, String placeId, String address) {
        if (storeName == null || storeName.isBlank()
                || placeId == null || placeId.isBlank()
                || address == null || address.isBlank()) {
            throw new IllegalArgumentException("storeName/placeId/address required");
        }
    }

    private String normalizeYn(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
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

    private VideoMediaResult processVideoUpload(MultipartFile file, MultipartFile thumbnailFile) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        if (file.getSize() > MAX_FILE_BYTES) {
            throw new IllegalArgumentException("file size exceeds 100MB limit");
        }

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "video.bin";
        String contentType = file.getContentType();
        validateFileType(originalName, contentType);

        File tempVideo = null;
        File transVideo = null;
        String fileUrl;
        Integer durationSeconds = null;
        byte[] thumbBytes = null;
        long size;
        try {
            tempVideo = File.createTempFile("upload-video-", ".tmp");
            file.transferTo(tempVideo);

            transVideo = ffmpegService.transcodeOptimized(tempVideo, 1280, 720, 2500, 128);
            File uploadTarget = transVideo != null ? transVideo : tempVideo;
            long uploadSize = uploadTarget.length();
            size = uploadSize;

            try (FileInputStream inputStream = new FileInputStream(uploadTarget)) {
                fileUrl = s3UploadService.uploadStreamWithPrefix(
                        s3UploadService.getVideoPrefix(),
                        originalName,
                        inputStream,
                        uploadSize,
                        "video/mp4"
                );
            }

            durationSeconds = ffmpegService.probeDurationSeconds(uploadTarget);
            if (durationSeconds != null && durationSeconds > MAX_DURATION_SECONDS) {
                throw new IllegalArgumentException("video duration exceeds 120 seconds");
            }
            thumbBytes = ffmpegService.generateThumbnail(uploadTarget, 1, 1280, -1, "jpg");
        } catch (IOException e) {
            throw new IllegalStateException("Failed to handle video upload", e);
        } finally {
            if (tempVideo != null && tempVideo.exists()) {
                tempVideo.delete();
            }
            if (transVideo != null && transVideo.exists()) {
                transVideo.delete();
            }
        }

        String thumbnailUrl = buildThumbnailPlaceholder(fileUrl);
        if (thumbnailFile != null && !thumbnailFile.isEmpty()) {
            try {
                String thumbName = safeFileName(thumbnailFile.getOriginalFilename(), "thumb.jpg");
                byte[] originalBytes = thumbnailFile.getBytes();
                byte[] optimized = imageProcessingService.resizeMax(originalBytes, 1280, 1280, "jpg");
                byte[] thumb300 = imageProcessingService.resizeCropCenter(originalBytes, 300, 300, "jpg");

                String optimizedUrl = s3UploadService.uploadBytesWithPrefix(
                        s3UploadService.getImagePrefix(),
                        "optimized-" + thumbName,
                        optimized,
                        "image/jpeg"
                );
                String thumb300Url = s3UploadService.uploadBytesWithPrefix(
                        s3UploadService.getImagePrefix(),
                        "thumb300-" + thumbName,
                        thumb300,
                        "image/jpeg"
                );
                thumbnailUrl = thumb300Url != null ? thumb300Url : optimizedUrl;
            } catch (IOException e) {
                log.warn("Failed to process thumbnail image: {}", e.getMessage());
            }
        } else if (thumbBytes != null && thumbBytes.length > 0) {
            try {
                String thumbName = safeFileName(originalName, "thumb.jpg");
                byte[] optimized = imageProcessingService.resizeMax(thumbBytes, 1280, 1280, "jpg");
                byte[] thumb300 = imageProcessingService.resizeCropCenter(thumbBytes, 300, 300, "jpg");

                String optimizedUrl = s3UploadService.uploadBytesWithPrefix(
                        s3UploadService.getImagePrefix(),
                        "optimized-" + thumbName,
                        optimized,
                        "image/jpeg"
                );
                String thumb300Url = s3UploadService.uploadBytesWithPrefix(
                        s3UploadService.getImagePrefix(),
                        "thumb300-" + thumbName,
                        thumb300,
                        "image/jpeg"
                );
                thumbnailUrl = thumb300Url != null ? thumb300Url : optimizedUrl;
            } catch (IOException e) {
                log.warn("Failed to generate thumbnail from video: {}", e.getMessage());
            }
        }

        return new VideoMediaResult(fileUrl, thumbnailUrl, durationSeconds, size);
    }

    private static final class VideoMediaResult {
        private final String fileUrl;
        private final String thumbnailUrl;
        private final Integer durationSeconds;
        private final long size;

        private VideoMediaResult(String fileUrl, String thumbnailUrl, Integer durationSeconds, long size) {
            this.fileUrl = fileUrl;
            this.thumbnailUrl = thumbnailUrl;
            this.durationSeconds = durationSeconds;
            this.size = size;
        }
    }

    private String buildThumbnailPlaceholder(String fileUrl) {
        String uuid = UUID.randomUUID().toString().replace("-", "");
        String today = LocalDate.now().toString();
        return fileUrl.replaceFirst("[^/]+$", "") + "thumb-" + today + "-" + uuid + ".jpg";
    }

    private String safeFileName(String original, String fallback) {
        if (original == null || original.isBlank()) {
            return fallback;
        }
        return original;
    }

    private void validateFileType(String originalName, String contentType) {
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > -1 && dotIndex + 1 < originalName.length()) {
            extension = originalName.substring(dotIndex + 1).toLowerCase();
        }
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("unsupported file extension");
        }
        if (contentType != null && !contentType.isBlank()
                && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("unsupported content type");
        }
    }
}
