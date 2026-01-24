package com.plateapp.plate_main.friend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SendFriendRequestRequest {
    @NotNull(message = "toUserId is required")
    private Integer toUserId;
}
