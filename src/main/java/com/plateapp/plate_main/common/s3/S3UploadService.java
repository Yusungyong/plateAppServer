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
    private final String imagePrefix;
    private final String profilePrefix;

    public S3UploadService(
            S3Client s3Client,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${aws.s3.base-url:https://s3.amazonaws.com}") String baseUrl,
            @Value("${aws.s3.imageFilePath:}") String imagePrefix,
            @Value("${aws.s3.profileImagePath:}") String profilePrefix,
            @Value("${aws.s3.region}") String region
    ) {
        this.s3Client = s3Client;
        this.bucket = bucket;
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.bucketHost = "https://" + bucket + ".s3." + region + ".amazonaws.com";
        this.imagePrefix = normalizePrefix(imagePrefix);
        this.profilePrefix = normalizePrefix(profilePrefix);
    }

    /**
     * 이미지 업로드용 헬퍼. key는 imageFilePath + 날짜/랜덤UUID/원본파일명 으로 구성.
     * @param originalFilename 원본 파일명 (확장자 포함)
     * @param inputStream 업로드 스트림 (호출 측에서 close)
     * @param contentLength 바이트 길이
     * @param contentType MIME 타입 (없으면 추정)
     * @return 공개 URL
     */
    public String uploadImage(String originalFilename, InputStream inputStream, long contentLength, String contentType) {
        String safeName = (originalFilename == null || originalFilename.isBlank())
                ? "upload.bin"
                : originalFilename;

        String key = buildImageKey(safeName);
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

    /** 프로필 이미지 업로드 (profileImagePath가 있으면 우선 사용, 없으면 일반 imagePrefix 사용) */
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

    private String buildImageKey(String filename) {
        return buildKeyWithPrefix(imagePrefix, filename);
    }

    private String buildKeyWithPrefix(String prefix, String filename) {
        String datePrefix = LocalDate.now().toString(); // YYYY-MM-DD
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
        // baseUrl 예: https://s3.amazonaws.com 또는 https://<bucket>.s3.<region>.amazonaws.com
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
