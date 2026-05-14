package com.orca.hrplatform.auth.dto;

import com.orca.hrplatform.auth.entity.Role;
import com.orca.hrplatform.auth.entity.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Data
@Builder
public class AdminUserResponse {
    private UUID id;
    private String email;
    private String status;
    private LocalDateTime lastLoginAt;
    private Integer failedLoginAttempts;
    private LocalDateTime lockedAt;
    private UUID employeeId;
    private Set<String> roles;

    public static AdminUserResponse from(User user) {
        return AdminUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .failedLoginAttempts(user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts())
                .lockedAt(user.getLockedAt())
                .employeeId(user.getEmployeeId())
                .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                .build();
    }
}
