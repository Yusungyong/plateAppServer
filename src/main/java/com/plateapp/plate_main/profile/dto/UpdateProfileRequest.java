package com.plateapp.plate_main.profile.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProfileRequest {
    @Size(min = 1, max = 20, message = "Nickname must be between 1 and 20 characters")
    private String nickname;

    @Size(max = 200, message = "Bio must be less than 200 characters")
    private String bio;

    private String activeRegion;

    @Email(message = "Invalid email format")
    private String email;

    private String phoneNumber;
}
