package com.plateapp.plate_main.common.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> data;
    private PaginationInfo pagination;

    public static <T> PagedResponse<T> of(List<T> data, long total, int limit, int offset) {
        PaginationInfo pagination = PaginationInfo.of(total, limit, offset);
        return new PagedResponse<>(data, pagination);
    }
}
