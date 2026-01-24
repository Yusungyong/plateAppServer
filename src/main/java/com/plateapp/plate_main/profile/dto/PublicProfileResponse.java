package com.plateapp.plate_main.profile.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class PublicProfileResponse {
    String username;
    String nickName;
    String profileImageUrl;
    String activeRegion;
}
