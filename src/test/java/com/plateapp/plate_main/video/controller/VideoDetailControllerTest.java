package com.plateapp.plate_main.video.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.plateapp.plate_main.common.api.ApiResponse;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import com.plateapp.plate_main.video.dto.VideoFeedItemDTO;
import com.plateapp.plate_main.video.service.VideoDetailService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
class VideoDetailControllerTest {

    @Mock
    private VideoDetailService videoDetailService;

    @Test
    void usesAuthenticatedViewerAndWrapsOneVideoInApiResponse() {
        VideoFeedItemDTO video = VideoFeedItemDTO.builder()
                .storeId(81)
                .username("author")
                .likedByMe(false)
                .build();
        when(videoDetailService.getVideo("viewer", 81)).thenReturn(video);
        VideoDetailController controller = new VideoDetailController(videoDetailService);

        TestingAuthenticationToken authentication = new TestingAuthenticationToken("viewer", null);
        authentication.setAuthenticated(true);

        ApiResponse<VideoFeedItemDTO> response = controller.getVideo(81, authentication);

        assertThat(response.isSuccess()).isTrue();
        assertThat(response.getData()).isSameAs(video);
        verify(videoDetailService).getVideo("viewer", 81);
    }

    @Test
    void rejectsMissingAuthentication() {
        VideoDetailController controller = new VideoDetailController(videoDetailService);

        assertThatThrownBy(() -> controller.getVideo(81, null))
                .isInstanceOfSatisfying(AppException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUTH_UNAUTHORIZED));
    }
}
