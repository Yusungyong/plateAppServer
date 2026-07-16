package com.plateapp.plate_main.video.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.common.s3.S3UploadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@ExtendWith(MockitoExtension.class)
class VideoPlaybackUrlServiceTest {

    @Mock
    private S3UploadService s3UploadService;
    @Mock
    private S3Presigner s3Presigner;

    @Test
    void publicVideoKeepsExistingDeliveryUrl() {
        when(s3UploadService.toVideoUrl("video.mp4"))
                .thenReturn("https://cdn.example.com/video/video.mp4");
        VideoPlaybackUrlService service = new VideoPlaybackUrlService(
                s3UploadService,
                s3Presigner,
                "plate-test",
                false
        );

        String result = service.resolvePlaybackUrl("video.mp4", true);

        assertThat(result).isEqualTo("https://cdn.example.com/video/video.mp4");
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void privateVideoUsesShortLivedPresignedUrlForOwnedObject() {
        when(s3UploadService.toVideoObjectKey("video.mp4")).thenReturn("video/video.mp4");

        try (S3Presigner localPresigner = S3Presigner.builder()
                .region(Region.AP_NORTHEAST_2)
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create("test-access-key", "test-secret-key")
                ))
                .build()) {
            VideoPlaybackUrlService service = new VideoPlaybackUrlService(
                    s3UploadService,
                    localPresigner,
                    "plate-test",
                    true
            );

            String result = service.resolvePlaybackUrl("video.mp4", false);

            assertThat(result)
                    .contains("video.mp4")
                    .contains("X-Amz-Expires=300")
                    .contains("X-Amz-Signature=");
        }
    }

    @Test
    void privateExternalUrlIsNotReturnedWhenObjectOwnershipCannotBeVerified() {
        String externalUrl = "https://external.example.com/video.mp4";
        when(s3UploadService.toVideoObjectKey(externalUrl)).thenReturn(null);
        VideoPlaybackUrlService service = new VideoPlaybackUrlService(
                s3UploadService,
                s3Presigner,
                "plate-test",
                true
        );

        String result = service.resolvePlaybackUrl(externalUrl, false);

        assertThat(result).isNull();
        verify(s3UploadService).toVideoObjectKey(externalUrl);
        verifyNoInteractions(s3Presigner);
    }

    @Test
    void privateVideoFailsClosedUntilDeliveryOriginIsVerified() {
        VideoPlaybackUrlService service = new VideoPlaybackUrlService(
                s3UploadService,
                s3Presigner,
                "plate-test",
                false
        );

        assertThat(service.resolvePlaybackUrl("video.mp4", false)).isNull();
        verifyNoInteractions(s3Presigner);
    }
}
