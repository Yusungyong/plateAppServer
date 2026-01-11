package com.plateapp.plate_main.friend.dto;

public class FriendRequests {

    public record CreateFriendRequest(
            String username,
            String friendName,
            String status,
            String initiatorUsername,
            String message
    ) {}

    public record UpdateStatusRequest(
            String status
    ) {}
}
