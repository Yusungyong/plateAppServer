package com.plateapp.plate_main.profile.dto;

public record DeleteAccountResponse(
        boolean success,
        String message,
        String errorCode
) {
    public static DeleteAccountResponse success(String message) {
        return new DeleteAccountResponse(true, message, null);
    }

    public static DeleteAccountResponse fail(String errorCode, String message) {
        return new DeleteAccountResponse(false, message, errorCode);
    }
}