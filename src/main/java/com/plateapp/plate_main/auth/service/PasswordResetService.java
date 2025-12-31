// src/main/java/com/plateapp/plate_main/auth/service/PasswordResetService.java
package com.plateapp.plate_main.auth.service;

public interface PasswordResetService {
    void resetPassword(String email, String newPassword);
}
