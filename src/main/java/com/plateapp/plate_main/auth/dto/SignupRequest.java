// src/main/java/com/plateapp/platemain/auth/dto/SignupRequest.java
package com.plateapp.plate_main.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    @NotBlank
    @Size(min = 4, max = 64)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @NotBlank
    private String nickname;

    public String getEmail() {
        return email;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getNickname() {
        return nickname;
    }
}
