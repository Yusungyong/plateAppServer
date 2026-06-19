package com.plateapp.plate_main.owner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class OwnerAccountValidationDtos {

    private OwnerAccountValidationDtos() {
    }

    public record Request(
            @NotBlank String field,
            @NotNull String value
    ) {
    }

    public record Response(
            String field,
            String value,
            boolean available,
            String message
    ) {
    }
}
