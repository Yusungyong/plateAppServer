package com.plateapp.plate_main.common.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;

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
    void rejectsExternalOrUnmanagedObjectLocations() {
        S3UploadService service = service(CDN_URL);

        assertThat(service.toObjectKey("https://attacker.example/" + OBJECT_KEY)).isNull();
        assertThat(service.toObjectKey(CDN_URL + "/unmanaged/secret.txt")).isNull();
        assertThat(service.toObjectKey("unmanaged/secret.txt")).isNull();
    }

    @Test
    void resolvesOnlyOwnedVideoKeysForPrivatePlayback() {
        S3UploadService service = service(CDN_URL);
        String videoKey = "foodvideos/2026-07-16/video.mp4";

        assertThat(service.toVideoObjectKey("2026-07-16/video.mp4")).isEqualTo(videoKey);
        assertThat(service.toVideoObjectKey(videoKey)).isEqualTo(videoKey);
        assertThat(service.toVideoObjectKey(CDN_URL + "/" + videoKey)).isEqualTo(videoKey);
        assertThat(service.toVideoObjectKey(CDN_URL + "/" + OBJECT_KEY)).isNull();
        assertThat(service.toVideoObjectKey("https://attacker.example/" + videoKey)).isNull();
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

    @Test
    void deletesOnlyUrlsFromConfiguredOrigins() {
        S3Client client = mock(S3Client.class);
        S3UploadService service = service(client, CDN_URL);

        service.deleteObjectByUrl(CDN_URL + "/" + OBJECT_KEY + "?version=1");

        verify(client).deleteObject(argThat((DeleteObjectRequest request) ->
                request.bucket().equals("foodplayerbucket") && request.key().equals(OBJECT_KEY)));
    }

    @Test
    void ignoresExternalUrlsAndLookalikeHosts() {
        S3Client client = mock(S3Client.class);
        S3UploadService service = service(client, CDN_URL);

        service.deleteObjectByUrl("https://attacker.example/" + OBJECT_KEY);
        service.deleteObjectByUrl("https://cdn.plateapp.example.evil/media/" + OBJECT_KEY);

        verify(client, never()).deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class));
    }

    @Test
    void ignoresTraversalKeys() {
        S3Client client = mock(S3Client.class);
        S3UploadService service = service(client, CDN_URL);

        service.deleteObjectByKey("../secret");
        service.deleteObjectByKey("foodimages/../../secret");
        service.deleteObjectByUrl(CDN_URL + "/%2e%2e/secret");

        verify(client, never()).deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class));
    }

    @Test
    void ignoresKeysOutsideManagedPrefixes() {
        S3Client client = mock(S3Client.class);
        S3UploadService service = service(client, CDN_URL);

        service.deleteObjectByKey("unmanaged/secret.txt");
        service.deleteObjectByUrl(CDN_URL + "/unmanaged/secret.txt");

        verify(client, never()).deleteObject(org.mockito.ArgumentMatchers.any(DeleteObjectRequest.class));
    }

    private S3UploadService service(String cdnBaseUrl) {
        return service(mock(S3Client.class), cdnBaseUrl);
    }

    private S3UploadService service(S3Client client, String cdnBaseUrl) {
        return new S3UploadService(
                client,
                "foodplayerbucket",
                BUCKET_URL,
                cdnBaseUrl,
                "foodvideos/",
                "foodimages/",
                "FeedImages/",
                "profileImage/",
                "thumbnail/",
                "restaurants/",
                "newsImage/",
                "ap-northeast-2"
        );
    }
}
