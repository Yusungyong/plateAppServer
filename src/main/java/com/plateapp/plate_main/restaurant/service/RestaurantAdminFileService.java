package com.plateapp.plate_main.restaurant.service;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import java.io.IOException;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class RestaurantAdminFileService {

    private static final long MAX_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final long MAX_VIDEO_BYTES = 100L * 1024 * 1024;
    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "mov", "webm");

    private final S3UploadService s3UploadService;

    @Value("${aws.s3.restaurantFilePath:restaurants/}")
    private String restaurantFilePrefix;

    public RestaurantAdminDtos.RestaurantFileUploadResponse upload(MultipartFile file) {
        validateFile(file);
        String contentType = contentTypeForExtension(extension(file.getOriginalFilename()));
        String objectKey = uploadToS3(file);
        return new RestaurantAdminDtos.RestaurantFileUploadResponse(
                s3UploadService.toDeliveryUrl(objectKey),
                file.getOriginalFilename(),
                contentType,
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

    private String uploadToS3(MultipartFile file) {
        String extension = extension(file.getOriginalFilename());
        String storedFilename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        try {
            return s3UploadService.uploadStreamKeyWithPrefix(
                    restaurantFilePrefix,
                    storedFilename,
                    file.getInputStream(),
                    file.getSize(),
                    contentTypeForExtension(extension)
            );
        } catch (IOException e) {
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR, "Restaurant file upload failed.");
        }
    }

    private String contentTypeForExtension(String extension) {
        return switch (extension) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "webp" -> "image/webp";
            case "gif" -> "image/gif";
            case "mp4" -> "video/mp4";
            case "mov" -> "video/quicktime";
            case "webm" -> "video/webm";
            default -> "application/octet-stream";
        };
    }
}
