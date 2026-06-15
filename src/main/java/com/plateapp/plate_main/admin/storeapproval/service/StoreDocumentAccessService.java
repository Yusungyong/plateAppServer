package com.plateapp.plate_main.admin.storeapproval.service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
@RequiredArgsConstructor
public class StoreDocumentAccessService {

    private static final Duration EXPIRY = Duration.ofSeconds(60);

    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket}")
    private String bucket;

    public PresignedDocument presign(String objectKey, String originalName, String purpose) {
        boolean download = "download".equals(purpose);
        String disposition = (download ? "attachment" : "inline")
                + "; filename*=UTF-8''"
                + URLEncoder.encode(originalName, StandardCharsets.UTF_8).replace("+", "%20");

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .responseContentDisposition(disposition)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(EXPIRY)
                .getObjectRequest(objectRequest)
                .build();

        return new PresignedDocument(
                s3Presigner.presignGetObject(presignRequest).url().toString(),
                OffsetDateTime.now(ZoneOffset.UTC).plus(EXPIRY)
        );
    }

    public record PresignedDocument(String accessUrl, OffsetDateTime expiresAt) {
    }
}
