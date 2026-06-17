package com.plateapp.plate_main.owner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.OffsetDateTime;

public final class BusinessVerificationDtos {

    private BusinessVerificationDtos() {
    }

    public record VerifyRequest(
            @NotBlank String businessNumber,
            @NotBlank @Size(max = 100) String representativeName,
            @NotNull LocalDate openingDate,
            @Size(max = 150) String businessName
    ) {
    }

    public record VerifyResponse(
            boolean verified,
            String verificationStatus,
            String message,
            String provider,
            OffsetDateTime verifiedAt
    ) {
    }
}
