package com.plateapp.plate_main.profile.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserStatsDTO {
    private long friendsCount;
    private long postsCount;
    private long likesCount;
    private long visitedStoresCount;
}
