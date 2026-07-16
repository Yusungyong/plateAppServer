package com.plateapp.plate_main.video.service;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.plateapp.plate_main.common.s3.S3UploadService;

import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Service
public class VideoPlaybackUrlService {

    private static final Duration PRIVATE_URL_EXPIRY = Duration.ofMinutes(5);

    private final S3UploadService s3UploadService;
    private final S3Presigner s3Presigner;
    private final String bucket;
    private final boolean privateMediaDeliveryReady;

    public VideoPlaybackUrlService(
            S3UploadService s3UploadService,
            S3Presigner s3Presigner,
            @Value("${aws.s3.bucket}") String bucket,
            @Value("${features.private-media-delivery-ready:false}") boolean privateMediaDeliveryReady
    ) {
        this.s3UploadService = s3UploadService;
        this.s3Presigner = s3Presigner;
        this.bucket = bucket;
        this.privateMediaDeliveryReady = privateMediaDeliveryReady;
    }

    /**
     * Public videos retain the existing delivery URL contract. Private videos
     * are only returned as short-lived URLs for objects owned by this service.
     */
    public String resolvePlaybackUrl(String storedPath, boolean publicVideo) {
        if (publicVideo) {
            return trimToNull(s3UploadService.toVideoUrl(storedPath));
        }
        if (!privateMediaDeliveryReady) {
            return null;
        }

        String objectKey = s3UploadService.toVideoObjectKey(storedPath);
        if (objectKey == null) {
            return null;
        }

        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .build();
        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(PRIVATE_URL_EXPIRY)
                .getObjectRequest(objectRequest)
                .build();
        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
