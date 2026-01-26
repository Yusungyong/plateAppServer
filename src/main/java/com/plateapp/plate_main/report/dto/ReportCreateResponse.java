package com.plateapp.plate_main.report.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ReportCreateResponse {
    boolean ok;
    Integer reportId;
}
