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
    @Email
    private String email;   // RN에서 id로 입력한 값

    @NotBlank
    @Size(min = 8, max = 64)
    private String password;

    @NotBlank
    private String nickname;
}
