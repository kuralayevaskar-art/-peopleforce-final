package com.orca.hrplatform.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
public class SecurityUtils {

    public static UUID getCurrentUserId() {
        // Implementation depends on how we stored the ID in Authentication
        // For now, I'll assume we might need to query or it's in the principal
        // But the user requested a static helper.
        // Usually, we use a custom UserPrincipal.
        return null; // Placeholder, see logic below
    }

    public static UUID getCurrentCompanyId() {
        // This is crucial for multi-tenancy.
        // We'll extract it from the authentication object if we stored it there.
        // In Phase 3, JwtAuthenticationFilter didn't store companyId in authorities.
        // We should probably add it to the principal.
        return null;
    }

    public static String getCurrentUserEmail() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (auth != null) ? auth.getName() : null;
    }

    public static List<String> getCurrentUserRoles() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) return Collections.emptyList();
        return auth.getAuthorities().stream()
                .map(a -> a.getAuthority().replace("ROLE_", ""))
                .collect(Collectors.toList());
    }

    public static boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }
}
