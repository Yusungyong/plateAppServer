package com.plateapp.plate_main.friend.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class FriendVisitCreateRequest {
    @NotNull
    LocalDate visitDate;
    @NotEmpty
    List<String> friends;
    Integer storeId;
    String storeName;
    String address;
    String memo;
}
