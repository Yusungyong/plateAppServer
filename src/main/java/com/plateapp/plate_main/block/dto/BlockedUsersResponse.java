package com.plateapp.plate_main.block.dto;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BlockedUsersResponse {
    List<BlockedUser> items;
    Long total;
    Integer offset;
    Integer limit;
}
