package com.plateapp.plate_main.common.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

class S3UploadServiceTest {

    private static final String BUCKET_URL =
            "https://foodplayerbucket.s3.ap-northeast-2.amazonaws.com";
    private static final String CDN_URL = "https://cdn.plateapp.example/media";
    private static final String OBJECT_KEY = "restaurants/2026-06-12/video.mp4";

    @Test
    void convertsS3AndCdnUrlsToTheSameObjectKey() {
        S3UploadService service = service(CDN_URL + "/");

        assertThat(service.toObjectKey(BUCKET_URL + "/" + OBJECT_KEY)).isEqualTo(OBJECT_KEY);
        assertThat(service.toObjectKey(CDN_URL + "/" + OBJECT_KEY + "?version=1")).isEqualTo(OBJECT_KEY);
        assertThat(service.toObjectKey(OBJECT_KEY)).isEqualTo(OBJECT_KEY);
    }

    @Test
    void buildsDeliveryUrlWithConfiguredCdnBaseUrl() {
        S3UploadService service = service(CDN_URL);

        assertThat(service.toDeliveryUrl(BUCKET_URL + "/" + OBJECT_KEY))
                .isEqualTo(CDN_URL + "/" + OBJECT_KEY);
        assertThat(service.toDeliveryUrl(OBJECT_KEY))
                .isEqualTo(CDN_URL + "/" + OBJECT_KEY);
    }

    @Test
    void fallsBackToS3UrlWhenCdnBaseUrlIsNotConfigured() {
        S3UploadService service = service("");

        assertThat(service.toDeliveryUrl(OBJECT_KEY))
                .isEqualTo(BUCKET_URL + "/" + OBJECT_KEY);
    }

    private S3UploadService service(String cdnBaseUrl) {
        return new S3UploadService(
                mock(S3Client.class),
                "foodplayerbucket",
                BUCKET_URL,
                cdnBaseUrl,
                "foodvideos/",
                "foodimages/",
                "FeedImages/",
                "profileImage/",
                "thumbnail/",
                "ap-northeast-2"
        );
    }
}
