package com.plateapp.plate_main.video.service;

import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.video.dto.VideoUploadResponse;
import com.plateapp.plate_main.video.entity.Fp300Store;
import com.plateapp.plate_main.video.repository.Fp300StoreRepository;
import com.plateapp.plate_main.common.image.ImageProcessingService;
import com.plateapp.plate_main.common.video.FfmpegService;
import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class VideoUploadService {

    private final S3UploadService s3UploadService;
    private final Fp300StoreRepository fp300StoreRepository;
    private final ImageProcessingService imageProcessingService;
    private final FfmpegService ffmpegService;

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
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        String safeOpenYn = (openYn == null || openYn.isBlank()) ? "Y" : openYn;
        String safeUseYn = (useYn == null || useYn.isBlank()) ? "Y" : useYn;
        String safeMuteYn = (muteYn == null || muteYn.isBlank()) ? "N" : muteYn;

        String originalName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "video.bin";
        String contentType = file.getContentType();

        File tempVideo = null;
        File transVideo = null;
        String fileUrl;
        Integer durationSeconds = null;
        byte[] thumbBytes = null;
        long size;
        try {
            tempVideo = File.createTempFile("upload-video-", ".tmp");
            file.transferTo(tempVideo);
            size = tempVideo.length();

            // 트랜스코딩 (H.264, 1280x720, 2500kbps, audio 128kbps)
            transVideo = ffmpegService.transcodeOptimized(tempVideo, 1280, 720, 2500, 128);
            File uploadTarget = transVideo != null ? transVideo : tempVideo;
            long uploadSize = uploadTarget.length();

            // 영상 업로드
            fileUrl = s3UploadService.uploadStreamWithPrefix(
                    s3UploadService.getVideoPrefix(),
                    originalName,
                    new FileInputStream(uploadTarget),
                    uploadSize,
                    "video/mp4"
            );

            // 길이 추출
            durationSeconds = ffmpegService.probeDurationSeconds(uploadTarget);
            // 썸네일 생성 (1초 지점 기준)
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

        Fp300Store store = new Fp300Store();
        Long newId = fp300StoreRepository.nextStoreIdFallback();
        if (newId != null) {
            store.setStoreId(newId.intValue());
        }
        store.setTitle(storeName);
        store.setFileName(fileUrl);
        store.setAddress(address);
        store.setUsername(username);
        store.setCreatedAt(LocalDate.now());
        store.setUpdatedAt(LocalDate.now());
        store.setThumbnail(thumbnailUrl);
        store.setStoreName(storeName);
        store.setOpenYn(safeOpenYn);
        store.setUseYn(safeUseYn);
        store.setPlaceId(placeId);
        store.setMuteYn(safeMuteYn);
        store.setVideoDuration(durationSeconds);
        store.setVideoSize(java.math.BigDecimal.valueOf(size));

        Fp300Store saved = fp300StoreRepository.save(store);

        return VideoUploadResponse.builder()
                .storeId(saved.getStoreId())
                .fileName(saved.getFileName())
                .thumbnail(saved.getThumbnail())
                .videoDuration(saved.getVideoDuration())
                .videoSize(saved.getVideoSize() != null ? saved.getVideoSize().longValue() : null)
                .build();
    }

    private String buildThumbnailPlaceholder(String fileUrl) {
        // 실 썸네일 생성 로직이 없다면, 파일명 기반 placeholder 경로를 만든다.
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
}
