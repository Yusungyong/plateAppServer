package com.plateapp.plate_main.common.s3;

import java.io.InputStream;
import java.net.URLConnection;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
public class S3UploadService {

    private final S3Client s3Client;
    private final String bucket;
    private final String baseUrl;
    private final String bucketHost;
    private final String videoPrefix;
    private final String imagePrefix;
    private final String profilePrefix;
    private final String thumbnailPrefix;

    public S3UploadService(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.base-url:https://s3.amazonaws.com}") String baseUrl,
            @Value("${aws.s3.videoFilePath:}") String videoPrefix,
            @Value("${aws.s3.imageFilePath:}") String imagePrefix,
            @Value("${aws.s3.profileImagePath:}") String profilePrefix,
            @Value("${aws.s3.thumbnailPath:thumbnail/}") String thumbnailPrefix,
            @Value("${aws.s3.region}") String region
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.bucketHost = "https://" + bucket + ".s3." + region + ".amazonaws.com";
        this.videoPrefix = normalizePrefix(videoPrefix);
        this.imagePrefix = normalizePrefix(imagePrefix);
        this.profilePrefix = normalizePrefix(profilePrefix);
        this.thumbnailPrefix = normalizePrefix(thumbnailPrefix);
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
        return buildPublicUrl(key);
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

    public String getThumbnailPrefix() {
        return thumbnailPrefix;
    }

    public String getVideoPrefix() {
        return videoPrefix;
    }

    public String getImagePrefix() {
        return imagePrefix;
    }

    private String buildKeyWithPrefix(String prefix, String filename) {
        String datePrefix = LocalDate.now().toString();
        String uuid = UUID.randomUUID().toString().replace("-", "");
        return prefix + datePrefix + "/" + uuid + "/" + filename;
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
}
