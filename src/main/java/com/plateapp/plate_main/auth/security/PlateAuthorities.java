package com.plateapp.plate_main.auth.security;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;

public final class PlateAuthorities {

    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";
    public static final String ROLE_OPERATOR = "OPERATOR";
    public static final String ROLE_USER = "USER";

    public static final String AUTHORITY_ADMIN = "ROLE_ADMIN";
    public static final String PERMISSION_ADMIN_ACCESS = "ADMIN_ACCESS";
    public static final String PERMISSION_FAQ_MANAGE = "FAQ_MANAGE";
    public static final String PERMISSION_QNA_MANAGE = "QNA_MANAGE";
    public static final String PERMISSION_MEMBER_MONITORING_READ = "MEMBER_MONITORING_READ";

    private PlateAuthorities() {
    }

    public static List<String> rolesFor(String rawRole) {
        return List.of(toRole(rawRole));
    }

    public static List<String> permissionsFor(String rawRole) {
        String role = toRole(rawRole);
        if (ROLE_ADMIN.equals(role) || ROLE_SUPER_ADMIN.equals(role)) {
            return List.of(PERMISSION_ADMIN_ACCESS);
        }
        return List.of();
    }

    public static String toRole(String rawRole) {
        if (rawRole == null || rawRole.isBlank()) {
            return ROLE_USER;
        }

        String normalized = rawRole.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }

        return switch (normalized) {
            case "ADM", "ADMIN", "993" -> ROLE_ADMIN;
            case "SUPER_ADMIN", "SUPERADMIN" -> ROLE_SUPER_ADMIN;
            case "OP", "OPS", "OPERATOR", "992" -> ROLE_OPERATOR;
            case "USR", "USER" -> ROLE_USER;
            default -> normalized;
        };
    }

    public static String toRoleAuthority(String rawRole) {
        return "ROLE_" + toRole(rawRole);
    }

    public static List<String> toSpringAuthorities(Collection<String> roles, Collection<String> permissions) {
        Set<String> authorities = new LinkedHashSet<>();
        if (roles != null) {
            roles.stream()
                    .map(PlateAuthorities::toRoleAuthority)
                    .forEach(authorities::add);
            if (roles.stream().map(PlateAuthorities::toRole).anyMatch(ROLE_SUPER_ADMIN::equals)) {
                authorities.add(PERMISSION_ADMIN_ACCESS);
            }
        }
        if (permissions != null) {
            permissions.stream()
                    .filter(permission -> permission != null && !permission.isBlank())
                    .map(permission -> permission.trim().toUpperCase(Locale.ROOT))
                    .forEach(authorities::add);
        }
        if (authorities.isEmpty()) {
            authorities.add("ROLE_USER");
        }
        return new ArrayList<>(authorities);
    }

    public static boolean hasAny(Authentication authentication, String... requiredAuthorities) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }

        Set<String> required = Set.of(requiredAuthorities);
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(required::contains);
    }
}
