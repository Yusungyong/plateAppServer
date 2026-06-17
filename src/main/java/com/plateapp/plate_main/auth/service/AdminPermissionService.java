package com.plateapp.plate_main.auth.service;

import com.plateapp.plate_main.auth.domain.AdminUserPermission;
import com.plateapp.plate_main.auth.domain.User;
import com.plateapp.plate_main.auth.repository.AdminUserPermissionRepository;
import com.plateapp.plate_main.auth.security.PlateAuthorities;
import com.plateapp.plate_main.owner.repository.StoreOwnerRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminPermissionService {

    private final AdminUserPermissionRepository permissionRepository;
    private final StoreOwnerRepository storeOwnerRepository;

    @Transactional(readOnly = true)
    public List<String> resolvePermissions(User user) {
        if (user == null) {
            return List.of();
        }

        List<String> permissions;
        Integer userId = user.getUserId();
        if (userId != null && permissionRepository.existsByUserId(userId)) {
            permissions = permissionRepository.findByUserIdAndRevokedAtIsNullOrderByPermissionAsc(userId).stream()
                    .map(AdminUserPermission::getPermission)
                    .distinct()
                    .toList();
        } else {
            permissions = PlateAuthorities.defaultPermissionsFor(user.getRole());
        }

        LinkedHashSet<String> resolved = new LinkedHashSet<>(permissions);
        if (hasActiveStoreOwner(user)) {
            resolved.add(PlateAuthorities.PERMISSION_OWNER_ACCESS);
        }
        return new ArrayList<>(resolved);
    }

    @Transactional(readOnly = true)
    public List<String> resolveRoles(User user) {
        if (user == null) {
            return List.of();
        }
        LinkedHashSet<String> roles = new LinkedHashSet<>(PlateAuthorities.rolesFor(user.getRole()));
        if (hasActiveStoreOwner(user)) {
            roles.add(PlateAuthorities.ROLE_STORE_OWNER);
        }
        return new ArrayList<>(roles);
    }

    private boolean hasActiveStoreOwner(User user) {
        Integer userId = user.getUserId();
        return userId != null && storeOwnerRepository.existsByUserIdAndRevokedAtIsNull(userId);
    }
}
