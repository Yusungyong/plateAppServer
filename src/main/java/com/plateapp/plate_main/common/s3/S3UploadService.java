package com.plateapp.plate_main.common.s3;

import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3UploadService {

    private final S3Client s3Client;
    private final String bucket;
    private final String baseUrl;
    private final String cdnBaseUrl;
    private final String bucketHost;
    private final String videoPrefix;
    private final String imagePrefix;
    private final String feedImagePrefix;
    private final String profilePrefix;
    private final String thumbnailPrefix;
    private final String restaurantPrefix;
    private final String newsImagePrefix;

    public S3UploadService(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.base-url:https://s3.amazonaws.com}") String baseUrl,
            @Value("${aws.s3.cdn-base-url:}") String cdnBaseUrl,
            @Value("${aws.s3.videoFilePath:}") String videoPrefix,
            @Value("${aws.s3.imageFilePath:}") String imagePrefix,
            @Value("${aws.s3.feedImagePath:}") String feedImagePrefix,
            @Value("${aws.s3.profileImagePath:}") String profilePrefix,
            @Value("${aws.s3.thumbnailPath:thumbnail/}") String thumbnailPrefix,
            @Value("${aws.s3.restaurantFilePath:restaurants/}") String restaurantPrefix,
            @Value("${aws.s3.newsImagePath:newsImage/}") String newsImagePrefix,
            @Value("${aws.s3.region}") String region
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.baseUrl = normalizeBaseUrl(baseUrl);
        this.cdnBaseUrl = normalizeBaseUrl(cdnBaseUrl);
        this.bucketHost = "https://" + bucket + ".s3." + region + ".amazonaws.com";
        this.videoPrefix = normalizePrefix(videoPrefix);
        this.imagePrefix = normalizePrefix(imagePrefix);
        this.feedImagePrefix = normalizePrefix(feedImagePrefix);
        this.profilePrefix = normalizePrefix(profilePrefix);
        this.thumbnailPrefix = normalizePrefix(thumbnailPrefix);
        this.restaurantPrefix = normalizePrefix(restaurantPrefix);
        this.newsImagePrefix = normalizePrefix(newsImagePrefix);
    }

    public String uploadImage(String originalFilename, InputStream inputStream, long contentLength, String contentType) {
        String safeName = (originalFilename == null || originalFilename.isBlank())
                ? "upload.bin"
                : originalFilename;

        String key = buildKeyWithPrefix(imagePrefix, safeName);
        String resolvedContentType = resolveContentType(safeName, contentType);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength(contentLength)
                .contentType(resolvedContentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));

        return buildPublicUrl(key);
    }

    public String uploadStreamWithPrefix(String prefix, String originalFilename, InputStream inputStream, long contentLength, String contentType) {
        String key = uploadStreamKeyWithPrefix(prefix, originalFilename, inputStream, contentLength, contentType);
        return buildPublicUrl(key);
    }

    public String uploadStreamKeyWithPrefix(
            String prefix,
            String originalFilename,
            InputStream inputStream,
            long contentLength,
            String contentType
    ) {
        String safeName = (originalFilename == null || originalFilename.isBlank())
                ? "upload.bin"
                : originalFilename;

        String key = buildKeyWithPrefix(normalizePrefix(prefix), safeName);
        String resolvedContentType = resolveContentType(safeName, contentType);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength(contentLength)
                .contentType(resolvedContentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
        return key;
    }

    public String uploadProfileImage(String originalFilename, InputStream inputStream, long contentLength, String contentType) {
        String safeName = (originalFilename == null || originalFilename.isBlank())
                ? "profile.bin"
                : originalFilename;

        String key = buildKeyWithPrefix(profilePrefix.isEmpty() ? imagePrefix : profilePrefix, safeName);
        String resolvedContentType = resolveContentType(safeName, contentType);

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength(contentLength)
                .contentType(resolvedContentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
        return buildPublicUrl(key);
    }

    public String uploadBytesWithPrefix(String prefix, String filename, byte[] data, String contentType) {
        String key = buildKeyWithPrefix(normalizePrefix(prefix), filename);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength((long) data.length)
                .contentType(resolveContentType(filename, contentType))
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(data));
        return buildPublicUrl(key);
    }

    public String uploadBytesWithPrefixAndPath(String prefix, String relativePath, byte[] data, String contentType) {
        String key = buildKeyWithPath(normalizePrefix(prefix), relativePath);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength((long) data.length)
                .contentType(resolveContentType(relativePath, contentType))
                .build();
        s3Client.putObject(putRequest, RequestBody.fromBytes(data));
        return key;
    }

    public String uploadStreamWithPrefixAndPath(
            String prefix,
            String relativePath,
            InputStream inputStream,
            long contentLength,
            String contentType
    ) {
        String key = buildKeyWithPath(normalizePrefix(prefix), relativePath);
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentLength(contentLength)
                .contentType(resolveContentType(relativePath, contentType))
                .build();
        s3Client.putObject(putRequest, RequestBody.fromInputStream(inputStream, contentLength));
        return key;
    }

    public void deleteObjectByUrl(String objectUrl) {
        String key = extractOwnedKey(objectUrl);
        if (key == null) {
            return;
        }
        deleteObjectByKey(key);
    }

    public void deleteObjectByKey(String key) {
        String safeKey = normalizeManagedKey(key);
        if (safeKey == null) {
            return;
        }
        DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(safeKey)
                .build();
        s3Client.deleteObject(deleteRequest);
    }

    public String getFeedImagePrefix() {
        return feedImagePrefix.isEmpty() ? imagePrefix : feedImagePrefix;
    }

    public String getThumbnailPrefix() {
        return thumbnailPrefix;
    }

    public String getVideoPrefix() {
        return videoPrefix;
    }

    public String getImagePrefix() {
        return imagePrefix;
    }

    public String toStoredVideoPath(String objectUrlOrKey) {
        String key = extractKey(objectUrlOrKey);
        if (key == null || key.isBlank()) {
            return objectUrlOrKey;
        }
        if (key.startsWith(videoPrefix)) {
            return key.substring(videoPrefix.length());
        }
        return key;
    }

    public String toStoredImagePath(String objectUrlOrKey) {
        String key = extractKey(objectUrlOrKey);
        if (key == null || key.isBlank()) {
            return objectUrlOrKey;
        }
        if (!imagePrefix.isEmpty() && key.startsWith(imagePrefix)) {
            return key.substring(imagePrefix.length());
        }
        if (!thumbnailPrefix.isEmpty() && key.startsWith(thumbnailPrefix)) {
            return key.substring(thumbnailPrefix.length());
        }
        return key;
    }

    public String toVideoUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return storedPath;
        }
        if (storedPath.contains("://")) {
            return storedPath;
        }
        String key = storedPath.startsWith(videoPrefix) ? storedPath : videoPrefix + storedPath;
        return buildPublicUrl(key);
    }

    public String toImageUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return storedPath;
        }
        if (storedPath.contains("://")) {
            return storedPath;
        }
        String key;
        if (!imagePrefix.isEmpty() && storedPath.startsWith(imagePrefix)) {
            key = storedPath;
        } else if (!thumbnailPrefix.isEmpty() && storedPath.startsWith(thumbnailPrefix)) {
            key = storedPath;
        } else {
            key = imagePrefix + storedPath;
        }
        return buildPublicUrl(key);
    }

    public String toFeedImageUrl(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return storedPath;
        }
        if (storedPath.contains("://")) {
            return storedPath;
        }
        String feedPrefix = getFeedImagePrefix();
        String key = storedPath.startsWith(feedPrefix) ? storedPath : feedPrefix + storedPath;
        return buildPublicUrl(key);
    }

    public String toObjectKey(String objectUrlOrKey) {
        return extractOwnedKey(objectUrlOrKey);
    }

    public String toDeliveryUrl(String objectUrlOrKey) {
        if (objectUrlOrKey == null || objectUrlOrKey.isBlank()) {
            return objectUrlOrKey;
        }
        String key = extractKey(objectUrlOrKey);
        if (key == null || key.isBlank()) {
            return objectUrlOrKey;
        }
        if (!cdnBaseUrl.isBlank()) {
            return cdnBaseUrl + "/" + key;
        }
        return buildPublicUrl(key);
    }

    public void deleteVideoObject(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        if (storedPath.contains("://")) {
            deleteObjectByUrl(storedPath);
            return;
        }
        String key = storedPath.startsWith(videoPrefix) ? storedPath : videoPrefix + storedPath;
        deleteObjectByKey(key);
    }

    public void deleteImageObject(String storedPath) {
        if (storedPath == null || storedPath.isBlank()) {
            return;
        }
        if (storedPath.contains("://")) {
            deleteObjectByUrl(storedPath);
            return;
        }
        String key;
        if (!imagePrefix.isEmpty() && storedPath.startsWith(imagePrefix)) {
            key = storedPath;
        } else if (!thumbnailPrefix.isEmpty() && storedPath.startsWith(thumbnailPrefix)) {
            key = storedPath;
        } else {
            key = imagePrefix + storedPath;
        }
        deleteObjectByKey(key);
    }

    private String buildKeyWithPrefix(String prefix, String filename) {
        String datePrefix = LocalDate.now().toString();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + datePrefix + "/" + uuid + "/" + filename;
    }

    private String buildKeyWithPath(String prefix, String relativePath) {
        String cleaned = relativePath == null ? "" : relativePath;
        if (cleaned.startsWith("/")) {
            cleaned = cleaned.substring(1);
        }
        return prefix + cleaned;
    }

    private String resolveContentType(String filename, String contentType) {
        if (StringUtils.hasText(contentType)) {
            return contentType;
        }
        String guessed = URLConnection.guessContentTypeFromName(filename);
        return guessed != null ? guessed : "application/octet-stream";
    }

    private String buildPublicUrl(String key) {
        if (baseUrl.contains(bucket)) {
            return baseUrl + "/" + key;
        }
        return bucketHost + "/" + key;
    }

    private String extractKey(String objectUrl) {
        if (objectUrl == null || objectUrl.isBlank()) {
            return null;
        }
        if (!objectUrl.contains("://")) {
            return objectUrl.startsWith("/") ? objectUrl.substring(1) : objectUrl;
        }
        String cdnKey = stripBaseUrl(objectUrl, cdnBaseUrl);
        if (cdnKey != null) {
            return cdnKey;
        }
        String baseUrlKey = stripBaseUrl(objectUrl, baseUrl);
        if (baseUrlKey != null) {
            return baseUrlKey;
        }
        String bucketHostKey = stripBaseUrl(objectUrl, bucketHost);
        if (bucketHostKey != null) {
            return bucketHostKey;
        }
        try {
            URI uri = URI.create(objectUrl);
            String path = uri.getPath();
            if (path == null || path.isBlank()) {
                return null;
            }
            return path.startsWith("/") ? path.substring(1) : path;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Resolve a key only when the caller supplied a raw object key or a URL that
     * belongs to one of this application's configured delivery origins.  The
     * permissive {@link #extractKey(String)} method is intentionally retained for
     * read/display conversion of legacy URLs, but must not be used for deletion.
     */
    private String extractOwnedKey(String objectUrlOrKey) {
        if (objectUrlOrKey == null || objectUrlOrKey.isBlank()) {
            return null;
        }
        if (!objectUrlOrKey.contains("://")) {
            return normalizeManagedKey(objectUrlOrKey);
        }

        String key = stripBaseUrl(objectUrlOrKey, cdnBaseUrl);
        if (key == null && baseUrl.contains(bucket)) {
            key = stripBaseUrl(objectUrlOrKey, baseUrl);
        }
        if (key == null) {
            key = stripBaseUrl(objectUrlOrKey, bucketHost);
        }
        return normalizeManagedKey(key);
    }

    private String stripBaseUrl(String objectUrl, String candidateBaseUrl) {
        if (candidateBaseUrl == null || candidateBaseUrl.isBlank()) {
            return null;
        }
        try {
            URI objectUri = URI.create(objectUrl);
            URI baseUri = URI.create(candidateBaseUrl);
            if (!sameOrigin(objectUri, baseUri)) {
                return null;
            }

            String objectPath = objectUri.getPath();
            String basePath = baseUri.getPath();
            if (objectPath == null) {
                return null;
            }
            String normalizedBasePath = basePath == null ? "" : basePath;
            while (normalizedBasePath.endsWith("/") && normalizedBasePath.length() > 1) {
                normalizedBasePath = normalizedBasePath.substring(0, normalizedBasePath.length() - 1);
            }
            String requiredPrefix = normalizedBasePath.isBlank() || "/".equals(normalizedBasePath)
                    ? "/"
                    : normalizedBasePath + "/";
            if (!objectPath.startsWith(requiredPrefix)) {
                return null;
            }
            String key = objectPath.substring(requiredPrefix.length());
            return key.isBlank() ? null : key;
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private boolean sameOrigin(URI left, URI right) {
        return left.getScheme() != null
                && right.getScheme() != null
                && left.getHost() != null
                && right.getHost() != null
                && left.getScheme().equalsIgnoreCase(right.getScheme())
                && left.getHost().equalsIgnoreCase(right.getHost())
                && effectivePort(left) == effectivePort(right);
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private String normalizeSafeKey(String key) {
        if (key == null || key.isBlank()) {
            return null;
        }
        String normalized = key.startsWith("/") ? key.substring(1) : key;
        String lower = normalized.toLowerCase(java.util.Locale.ROOT);
        if (normalized.isBlank()
                || normalized.contains("://")
                || normalized.indexOf('\\') >= 0
                || normalized.chars().anyMatch(Character::isISOControl)
                || lower.contains("%2e")
                || lower.contains("%2f")
                || lower.contains("%5c")) {
            return null;
        }
        for (String segment : normalized.split("/", -1)) {
            if (segment.equals(".") || segment.equals("..")) {
                return null;
            }
        }
        return normalized;
    }

    private String normalizeManagedKey(String key) {
        String safeKey = normalizeSafeKey(key);
        if (safeKey == null) {
            return null;
        }
        return hasPrefix(safeKey, videoPrefix)
                || hasPrefix(safeKey, imagePrefix)
                || hasPrefix(safeKey, feedImagePrefix)
                || hasPrefix(safeKey, profilePrefix)
                || hasPrefix(safeKey, thumbnailPrefix)
                || hasPrefix(safeKey, restaurantPrefix)
                || hasPrefix(safeKey, newsImagePrefix)
                ? safeKey
                : null;
    }

    private boolean hasPrefix(String key, String prefix) {
        return prefix != null && !prefix.isBlank() && key.startsWith(prefix);
    }

    private String normalizePrefix(String prefix) {
        if (prefix == null || prefix.isBlank()) {
            return "";
        }
        String cleaned = prefix.startsWith("/") ? prefix.substring(1) : prefix;
        if (!cleaned.endsWith("/")) {
            cleaned += "/";
        }
        return cleaned;
    }

    private String normalizeBaseUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String normalized = value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
