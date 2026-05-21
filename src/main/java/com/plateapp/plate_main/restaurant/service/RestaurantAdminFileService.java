package com.plateapp.plate_main.restaurant.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.file.LocalFileStorageService;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import java.util.Locale;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RestaurantAdminFileService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "webm");

    private final LocalFileStorageService localFileStorageService;

    public RestaurantAdminDtos.RestaurantFileUploadResponse upload(MultipartFile file) {
        validateFile(file);
        String fileUrl = localFileStorageService.storeRestaurantFile(file);
        return new RestaurantAdminDtos.RestaurantFileUploadResponse(
                fileUrl,
                file.getOriginalFilename(),
                file.getContentType(),
                file.getSize()
        );
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Upload file is required.");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        String extension = extension(file.getOriginalFilename());
        boolean image = contentType.startsWith("image/") && IMAGE_EXTENSIONS.contains(extension);
        boolean video = contentType.startsWith("video/") && VIDEO_EXTENSIONS.contains(extension);
        if (!image && !video) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Only image or video files are allowed.");
        }
        if (image && file.getSize() > MAX_IMAGE_BYTES) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Image file must be 10MB or less.");
        }
        if (video && file.getSize() > MAX_VIDEO_BYTES) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Video file must be 100MB or less.");
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
}
