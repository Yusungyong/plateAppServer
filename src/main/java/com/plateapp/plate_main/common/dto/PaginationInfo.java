package com.plateapp.plate_main.common.dto;

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
public class PaginationInfo {
    private long total;
    private int limit;
    private int offset;
    private boolean hasMore;

    public static PaginationInfo of(long total, int limit, int offset) {
        boolean hasMore = (offset + limit) < total;
        return PaginationInfo.builder()
                .total(total)
                .limit(limit)
                .offset(offset)
                .hasMore(hasMore)
                .build();
    }
}
