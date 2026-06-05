package com.plateapp.plate_main.common.file;

import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class LocalFileStorageService {

    private static final long MAX_SEASONAL_IMAGE_BYTES = 10L * 1024 * 1024;
    private static final Set<String> SEASONAL_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> SEASONAL_IMAGE_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private final Path rootPath;
    private final String publicBasePath;

    public LocalFileStorageService(
            @Value("${file.upload.path}") String rootPath,
            @Value("${file.upload.public-base-path:/files}") String publicBasePath
    ) {
        this.rootPath = Paths.get(rootPath).toAbsolutePath().normalize();
        this.publicBasePath = normalizeBasePath(publicBasePath);
    }

    public String storeSeasonalImage(MultipartFile file, String bucket) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "업로드 파일이 비어 있습니다.");
        }
        String originalFilename = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
        String safeFilename = UUID.randomUUID().toString().replace("-", "") + "-" + sanitizeFilename(originalFilename);
        LocalDate today = LocalDate.now();
        Path relativePath = Paths.get(
                "seasonal",
                bucket,
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                safeFilename
        );
        Path targetPath = rootPath.resolve(relativePath).normalize();

        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return toPublicUrl(relativePath);
        } catch (IOException e) {
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR, "NAS 이미지 저장에 실패했습니다.");
        }
    }

    public String storeRestaurantFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Upload file is required.");
        }
        validateSeasonalImage(file);
        String originalFilename = file.getOriginalFilename() == null ? "upload.bin" : file.getOriginalFilename();
        String safeFilename = UUID.randomUUID().toString().replace("-", "") + "-" + sanitizeFilename(originalFilename);
        LocalDate today = LocalDate.now();
        Path relativePath = Paths.get(
                "restaurants",
                String.valueOf(today.getYear()),
                String.format("%02d", today.getMonthValue()),
                String.format("%02d", today.getDayOfMonth()),
                safeFilename
        );
        Path targetPath = rootPath.resolve(relativePath).normalize();

        try {
            Files.createDirectories(targetPath.getParent());
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return toPublicUrl(relativePath);
        } catch (IOException e) {
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR, "Restaurant file upload failed.");
        }
    }

    public void deleteByPublicUrl(String publicUrl) {
        if (publicUrl == null || publicUrl.isBlank()) {
            return;
        }
        Path relativePath = toRelativePath(publicUrl);
        if (relativePath == null) {
            return;
        }
        try {
            Files.deleteIfExists(rootPath.resolve(relativePath).normalize());
        } catch (IOException e) {
            throw new AppException(ErrorCode.COMMON_INTERNAL_ERROR, "NAS 이미지 삭제에 실패했습니다.");
        }
    }

    public String toPublicUrl(Path relativePath) {
        String unixPath = relativePath.toString().replace('\\', '/');
        return publicBasePath + "/" + unixPath;
    }

    private Path toRelativePath(String publicUrl) {
        String path = publicUrl;
        if (publicUrl.contains("://")) {
            try {
                path = URI.create(publicUrl).getPath();
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        if (path == null || path.isBlank()) {
            return null;
        }
        if (!path.startsWith(publicBasePath)) {
            return null;
        }
        String relative = path.substring(publicBasePath.length());
        if (relative.startsWith("/")) {
            relative = relative.substring(1);
        }
        return relative.isBlank() ? null : Paths.get(relative);
    }

    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private void validateSeasonalImage(MultipartFile file) {
        if (file.getSize() > MAX_SEASONAL_IMAGE_BYTES) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Seasonal image must be 10MB or less.");
        }

        String extension = extension(file.getOriginalFilename());
        if (!SEASONAL_IMAGE_EXTENSIONS.contains(extension)) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Only image files are allowed.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()
                && !SEASONAL_IMAGE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new AppException(ErrorCode.COMMON_INVALID_INPUT, "Only image files are allowed.");
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

    private String normalizeBasePath(String basePath) {
        if (basePath == null || basePath.isBlank()) {
            return "/files";
        }
        String normalized = basePath.startsWith("/") ? basePath : "/" + basePath;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}
