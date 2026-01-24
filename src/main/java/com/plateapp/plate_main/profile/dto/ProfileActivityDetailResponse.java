package com.plateapp.plate_main.profile.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ProfileActivityDetailResponse<T> {
    List<T> items;
    int limit;
    int offset;
    long total;
}
