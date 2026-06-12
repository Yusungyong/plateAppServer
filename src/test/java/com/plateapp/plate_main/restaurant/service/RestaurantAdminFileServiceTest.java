package com.plateapp.plate_main.restaurant.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.common.s3.S3UploadService;
import com.plateapp.plate_main.restaurant.dto.RestaurantAdminDtos;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RestaurantAdminFileServiceTest {

    @Mock
    private S3UploadService s3UploadService;

    @Test
    void uploadsMp4WithStandardContentTypeAndReturnsCdnUrl() {
        RestaurantAdminFileService service = new RestaurantAdminFileService(s3UploadService);
        ReflectionTestUtils.setField(service, "restaurantFilePrefix", "restaurants/");
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "preview.mp4",
                "video/x-m4v",
                new byte[] {0, 1, 2}
        );
        String objectKey = "restaurants/2026-06-12/video.mp4";
        String cdnUrl = "https://cdn.plateapp.example/" + objectKey;

        when(s3UploadService.uploadStreamKeyWithPrefix(
                eq("restaurants/"),
                anyString(),
                any(InputStream.class),
                anyLong(),
                eq("video/mp4")
        )).thenReturn(objectKey);
        when(s3UploadService.toDeliveryUrl(objectKey)).thenReturn(cdnUrl);

        RestaurantAdminDtos.RestaurantFileUploadResponse response = service.upload(file);

        assertThat(response.fileUrl()).isEqualTo(cdnUrl);
        assertThat(response.mimeType()).isEqualTo("video/mp4");
        verify(s3UploadService).uploadStreamKeyWithPrefix(
                eq("restaurants/"),
                anyString(),
                any(InputStream.class),
                eq(3L),
                eq("video/mp4")
        );
    }
}
