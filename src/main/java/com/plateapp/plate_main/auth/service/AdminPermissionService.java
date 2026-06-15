package com.plateapp.plate_main.auth.service;

import com.plateapp.plate_main.auth.domain.AdminUserPermission;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.AdminUserPermissionRepository;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private final AdminUserPermissionRepository permissionRepository;

    @Transactional(readOnly = true)
    public List<String> resolvePermissions(User user) {
        if (user == null) {
            return List.of();
        }

        Integer userId = user.getUserId();
        if (userId != null && permissionRepository.existsByUserId(userId)) {
            return permissionRepository.findByUserIdAndRevokedAtIsNullOrderByPermissionAsc(userId).stream()
                    .map(AdminUserPermission::getPermission)
                    .distinct()
                    .toList();
        }

        return PlateAuthorities.defaultPermissionsFor(user.getRole());
    }
}
