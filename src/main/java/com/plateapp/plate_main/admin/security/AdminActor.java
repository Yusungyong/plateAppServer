package com.plateapp.plate_main.admin.security;

public record AdminActor(
        Integer userId,
        String username,
        String role
) {
}
