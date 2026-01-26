package com.plateapp.plate_main.report.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReportHistoryResponse {
    List<ReportHistoryItem> items;
    Long total;
    Integer offset;
    Integer limit;
}
