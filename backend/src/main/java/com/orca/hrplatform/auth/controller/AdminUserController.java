package com.orca.hrplatform.auth.controller;

import com.orca.hrplatform.auth.dto.AdminUserResponse;
import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.entity.UserStatus;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.common.security.SecurityUtils;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        User current = requireAdmin();
        return ApiResponse.success(userRepository.findAll().stream()
                .filter(user -> current.getCompanyId().equals(user.getCompanyId()))
                .sorted(Comparator.comparing(User::getEmail, String.CASE_INSENSITIVE_ORDER))
                .map(AdminUserResponse::from)
                .toList());
    }

    @PostMapping("/{id}/block")
    public ApiResponse<AdminUserResponse> block(@PathVariable UUID id) {
        User current = requireAdmin();
        User target = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        ensureSameCompany(current, target);
        if (current.getId().equals(target.getId())) {
            throw new IllegalArgumentException("You cannot block your own user");
        }
        target.setStatus(UserStatus.BLOCKED);
        target.setLockedAt(java.time.LocalDateTime.now());
        return ApiResponse.success(AdminUserResponse.from(userRepository.save(target)), "User blocked");
    }

    @PostMapping("/{id}/unblock")
    public ApiResponse<AdminUserResponse> unblock(@PathVariable UUID id) {
        User current = requireAdmin();
        User target = userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));
        ensureSameCompany(current, target);
        target.setStatus(UserStatus.ACTIVE);
        target.setFailedLoginAttempts(0);
        target.setLockedAt(null);
        return ApiResponse.success(AdminUserResponse.from(userRepository.save(target)), "User unblocked");
    }

    private User requireAdmin() {
        User current = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        boolean admin = current.getRoles().stream()
                .anyMatch(role -> List.of("SUPER_ADMIN", "HR_ADMIN", "INTEGRATION_ADMIN").contains(role.getCode()));
        if (!admin) {
            throw new AccessDeniedException("Admin role required");
        }
        return current;
    }

    private void ensureSameCompany(User current, User target) {
        if (!current.getCompanyId().equals(target.getCompanyId())) {
            throw new AccessDeniedException("User belongs to another company");
        }
    }
}
