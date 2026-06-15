package com.plateapp.plate_main.admin.security;

import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.UserRepository;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.common.error.AppException;
import com.plateapp.plate_main.common.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdminActorResolver {

    private final UserRepository userRepository;

    public AdminActor resolve(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        User user = userRepository.findById(authentication.getName())
                .orElseThrow(() -> new AppException(ErrorCode.AUTH_UNAUTHORIZED));
        if (user.getUserId() == null) {
            throw new AppException(ErrorCode.AUTH_UNAUTHORIZED);
        }

        return new AdminActor(
                user.getUserId(),
                user.getUsername(),
                PlateAuthorities.toRole(user.getRole())
        );
    }
}
