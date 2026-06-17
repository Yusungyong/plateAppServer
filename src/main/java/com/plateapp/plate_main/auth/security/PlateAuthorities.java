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
    public static final String ROLE_CONTENT_MANAGER = "CONTENT_MANAGER";
    public static final String ROLE_VIEWER = "VIEWER";
    public static final String ROLE_USER = "USER";
    public static final String ROLE_STORE_OWNER = "STORE_OWNER";

    public static final String AUTHORITY_ADMIN = "ROLE_ADMIN";
    public static final String AUTHORITY_SUPER_ADMIN = "ROLE_SUPER_ADMIN";
    public static final String PERMISSION_ADMIN_ACCESS = "ADMIN_ACCESS";
    public static final String PERMISSION_DASHBOARD_READ = "DASHBOARD_READ";
    public static final String PERMISSION_STORE_READ = "STORE_READ";
    public static final String PERMISSION_STORE_APPROVE = "STORE_APPROVE";
    public static final String PERMISSION_STORE_UPDATE = "STORE_UPDATE";
    public static final String PERMISSION_FEED_READ = "FEED_READ";
    public static final String PERMISSION_FEED_MODERATE = "FEED_MODERATE";
    public static final String PERMISSION_FEED_FEATURE = "FEED_FEATURE";
    public static final String PERMISSION_SEASONAL_READ = "SEASONAL_READ";
    public static final String PERMISSION_SEASONAL_MANAGE = "SEASONAL_MANAGE";
    public static final String PERMISSION_REPORT_READ = "REPORT_READ";
    public static final String PERMISSION_BANNER_MANAGE = "BANNER_MANAGE";
    public static final String PERMISSION_NOTICE_MANAGE = "NOTICE_MANAGE";
    public static final String PERMISSION_SUPPORT_MANAGE = "SUPPORT_MANAGE";
    public static final String PERMISSION_ADMIN_ACCOUNT_MANAGE = "ADMIN_ACCOUNT_MANAGE";
    public static final String PERMISSION_SETTING_MANAGE = "SETTING_MANAGE";
    public static final String PERMISSION_AUDIT_LOG_READ = "AUDIT_LOG_READ";
    public static final String PERMISSION_FAQ_MANAGE = "FAQ_MANAGE";
    public static final String PERMISSION_QNA_MANAGE = "QNA_MANAGE";
    public static final String PERMISSION_MEMBER_MONITORING_READ = "MEMBER_MONITORING_READ";
    public static final String PERMISSION_RESTAURANT_MANAGE = "RESTAURANT_MANAGE";
    public static final String PERMISSION_OWNER_ACCESS = "OWNER_ACCESS";

    private static final List<String> ALL_ADMIN_PERMISSIONS = List.of(
            PERMISSION_ADMIN_ACCESS,
            PERMISSION_DASHBOARD_READ,
            PERMISSION_STORE_READ,
            PERMISSION_STORE_APPROVE,
            PERMISSION_STORE_UPDATE,
            PERMISSION_FEED_READ,
            PERMISSION_FEED_MODERATE,
            PERMISSION_FEED_FEATURE,
            PERMISSION_SEASONAL_READ,
            PERMISSION_SEASONAL_MANAGE,
            PERMISSION_REPORT_READ,
            PERMISSION_BANNER_MANAGE,
            PERMISSION_NOTICE_MANAGE,
            PERMISSION_SUPPORT_MANAGE,
            PERMISSION_ADMIN_ACCOUNT_MANAGE,
            PERMISSION_SETTING_MANAGE,
            PERMISSION_AUDIT_LOG_READ,
            PERMISSION_FAQ_MANAGE,
            PERMISSION_QNA_MANAGE,
            PERMISSION_MEMBER_MONITORING_READ,
            PERMISSION_RESTAURANT_MANAGE
    );

    private PlateAuthorities() {
    }

    public static List<String> rolesFor(String rawRole) {
        return List.of(toRole(rawRole));
    }

    public static List<String> permissionsFor(String rawRole) {
        return defaultPermissionsFor(rawRole);
    }

    public static List<String> defaultPermissionsFor(String rawRole) {
        return switch (toRole(rawRole)) {
            case ROLE_ADMIN, ROLE_SUPER_ADMIN -> ALL_ADMIN_PERMISSIONS;
            case ROLE_OPERATOR -> List.of(
                    PERMISSION_ADMIN_ACCESS,
                    PERMISSION_DASHBOARD_READ,
                    PERMISSION_STORE_READ,
                    PERMISSION_STORE_APPROVE,
                    PERMISSION_STORE_UPDATE,
                    PERMISSION_SUPPORT_MANAGE
            );
            case ROLE_CONTENT_MANAGER -> List.of(
                    PERMISSION_ADMIN_ACCESS,
                    PERMISSION_DASHBOARD_READ,
                    PERMISSION_FEED_READ,
                    PERMISSION_FEED_MODERATE,
                    PERMISSION_FEED_FEATURE,
                    PERMISSION_SEASONAL_READ,
                    PERMISSION_SEASONAL_MANAGE
            );
            case ROLE_VIEWER -> List.of(
                    PERMISSION_ADMIN_ACCESS,
                    PERMISSION_DASHBOARD_READ,
                    PERMISSION_STORE_READ,
                    PERMISSION_FEED_READ,
                    PERMISSION_SEASONAL_READ,
                    PERMISSION_REPORT_READ
            );
            default -> List.of();
        };
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
            case "CONTENT_MANAGER", "CONTENTMANAGER" -> ROLE_CONTENT_MANAGER;
            case "VIEWER", "VIEW_ONLY", "READ_ONLY" -> ROLE_VIEWER;
            case "STORE_OWNER", "STOREOWNER", "OWNER" -> ROLE_STORE_OWNER;
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

    public static boolean hasAdminAccess(Authentication authentication) {
        return hasAny(authentication, AUTHORITY_SUPER_ADMIN, PERMISSION_ADMIN_ACCESS);
    }

    public static boolean hasAdminPermission(Authentication authentication, String permission) {
        return hasAny(authentication, AUTHORITY_SUPER_ADMIN)
                || (hasAny(authentication, PERMISSION_ADMIN_ACCESS)
                && hasAny(authentication, permission));
    }
}
