// src/main/java/com/plateapp/plate_main/video/dto/VideoFeedItemDTO.java
package com.plateapp.plate_main.video.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VideoFeedItemDTO {

    private Integer storeId;
    private String placeId;

    private String title;        // 제목 (없으면 storeName으로 채움)
    private String storeName;    // 가게명
    private String address;      // 주소

    private String fileName;     // 동영상 파일명 (전체 URL 아님)
    private String thumbnail;    // 썸네일 파일명

    private Integer videoDuration; // 초 단위 재생시간

    // ✅ 기존
    private Long commentCount;       // 댓글 수 (fp_440)
    private String profileImageUrl;  // 작성자 프로필 이미지 (fp_100.profile_image_url)
    private String username;

    // ✅ 좋아요 추가
    private Long likeCount;          // 좋아요 수 (fp_50)
    private Boolean likedByMe;       // 내가 좋아요 눌렀는지
}
