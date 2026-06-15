package com.plateapp.plate_main.auth.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

class PlateAuthoritiesTest {

    @Test
    void detailedAdminPermissionRequiresAdminAccessToo() {
        var onlyDetailedPermission = authentication(PlateAuthorities.PERMISSION_STORE_APPROVE);
        var completePermissionSet = authentication(
                PlateAuthorities.PERMISSION_ADMIN_ACCESS,
                PlateAuthorities.PERMISSION_STORE_APPROVE
        );

        assertFalse(PlateAuthorities.hasAdminPermission(
                onlyDetailedPermission,
                PlateAuthorities.PERMISSION_STORE_APPROVE
        ));
        assertTrue(PlateAuthorities.hasAdminPermission(
                completePermissionSet,
                PlateAuthorities.PERMISSION_STORE_APPROVE
        ));
    }

    @Test
    void superAdminBypassesDetailedPermissionCheck() {
        var superAdmin = authentication(PlateAuthorities.AUTHORITY_SUPER_ADMIN);

        assertTrue(PlateAuthorities.hasAdminAccess(superAdmin));
        assertTrue(PlateAuthorities.hasAdminPermission(
                superAdmin,
                PlateAuthorities.PERMISSION_ADMIN_ACCOUNT_MANAGE
        ));
    }

    @Test
    void viewerPresetDoesNotIncludeWritePermissions() {
        List<String> permissions = PlateAuthorities.defaultPermissionsFor(PlateAuthorities.ROLE_VIEWER);

        assertTrue(permissions.contains(PlateAuthorities.PERMISSION_ADMIN_ACCESS));
        assertTrue(permissions.contains(PlateAuthorities.PERMISSION_STORE_READ));
        assertFalse(permissions.contains(PlateAuthorities.PERMISSION_STORE_APPROVE));
        assertFalse(permissions.contains(PlateAuthorities.PERMISSION_STORE_UPDATE));
    }

    private UsernamePasswordAuthenticationToken authentication(String... authorities) {
        return new UsernamePasswordAuthenticationToken(
                "admin",
                null,
                List.of(authorities).stream().map(SimpleGrantedAuthority::new).toList()
        );
    }
}
