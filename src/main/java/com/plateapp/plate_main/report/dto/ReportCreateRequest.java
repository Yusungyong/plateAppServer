package com.plateapp.plate_main.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReportCreateRequest {

    @NotBlank
    private String targetType;

    @NotNull
    private Integer targetId;

    private String targetUsername;

    @NotBlank
    private String reason;
}
