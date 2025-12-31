// src/main/java/com/plateapp/plate_main/video/dto/VideoWatchHistoryCreateRequest.java
package com.plateapp.plate_main.video.dto;

import lombok.Data;

@Data
public class VideoWatchHistoryCreateRequest {

    private Long storeId;      // 🔹 필수: 어떤 썸네일(가게)인지

    private String username;   // 🔹 로그인 유저라면 필수
                               //    (게스트일 때는 "GUEST_xxx" 같이 넣어도 됨)

    private Boolean isGuest;   // 🔹 true면 게스트 시청, false면 로그인 사용자
                               //    (null이면 false로 보고 처리해도 됨)

    private String guestId;    // 🔹 isGuest=true일 때 사용 (쿠키/디바이스 ID 등)
}
