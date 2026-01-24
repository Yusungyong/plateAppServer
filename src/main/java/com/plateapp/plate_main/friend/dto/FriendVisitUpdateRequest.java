package com.plateapp.plate_main.friend.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FriendVisitUpdateRequest {
    @NotNull
    LocalDate visitDate;
    String storeName;
    String address;
    String memo;
}
