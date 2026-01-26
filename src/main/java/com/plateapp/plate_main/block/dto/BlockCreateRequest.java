package com.plateapp.plate_main.block.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BlockCreateRequest {

    @NotBlank
    private String blockedUsername;
}
