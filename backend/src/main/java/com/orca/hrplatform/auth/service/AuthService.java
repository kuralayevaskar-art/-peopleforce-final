package com.orca.hrplatform.auth.service;

import com.orca.hrplatform.auth.dto.*;
import com.orca.hrplatform.auth.entity.RefreshToken;
import com.orca.hrplatform.auth.entity.Role;
import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.entity.UserStatus;
import com.orca.hrplatform.auth.repository.RoleRepository;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.audit.service.AuditLogService;
import com.orca.hrplatform.employee.entity.Employee;
import com.orca.hrplatform.employee.repository.EmployeeRepository;
import com.orca.hrplatform.integration.ad.config.AdProperties;
import com.orca.hrplatform.integration.ad.service.AdDirectoryService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final int MAX_FAILED_LOGIN_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final AdDirectoryService adDirectoryService;
    private final AdProperties adProperties;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public LoginResponse login(LoginRequest request) {
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BadCredentialsException("Account is not active");
            }

            if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
                registerFailedLogin(user);
                throw new BadCredentialsException("Invalid email or password");
            }

            user.setLastLoginAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            user.setLockedAt(null);
            userRepository.save(user);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = refreshTokenService.createRefreshToken(user);

            auditLogService.login(request.getEmail(), true, null, null);
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtService.getExpirationSeconds())
                    .user(convertToAuthUserResponse(user))
                    .build();
        } catch (RuntimeException ex) {
            auditLogService.login(request.getEmail(), false, ex.getMessage(), null);
            throw ex;
        }
    }

    @Transactional(noRollbackFor = BadCredentialsException.class)
    public LoginResponse loginWithAd(AdLoginRequest request) {
        try {
            AdDirectoryService.AdUser adUser;
            try {
                adUser = adDirectoryService.authenticate(request.getUsername(), request.getPassword());
            } catch (Exception ex) {
                registerFailedLogin(request.getUsername());
                throw new BadCredentialsException("Invalid AD username or password", ex);
            }
            User user = userRepository.findByEmail(adUser.getEmail())
                    .orElseGet(() -> createLocalUserFromAd(adUser));

            if (user.getStatus() != UserStatus.ACTIVE) {
                throw new BadCredentialsException("Account is not active");
            }

            user.setLastLoginAt(LocalDateTime.now());
            user.setFailedLoginAttempts(0);
            user.setLockedAt(null);
            userRepository.save(user);

            String accessToken = jwtService.generateAccessToken(user);
            String refreshToken = refreshTokenService.createRefreshToken(user);

            auditLogService.login(request.getUsername(), true, null, null);
            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .expiresIn(jwtService.getExpirationSeconds())
                    .user(convertToAuthUserResponse(user))
                    .build();
        } catch (RuntimeException ex) {
            auditLogService.login(request.getUsername(), false, ex.getMessage(), null);
            throw ex;
        }
    }

    @Transactional
    public RefreshTokenResponse refresh(RefreshTokenRequest request) {
        RefreshToken oldToken = refreshTokenService.validateRefreshToken(request.getRefreshToken())
                .orElseThrow(() -> new BadCredentialsException("Invalid or expired refresh token"));

        User user = userRepository.findById(oldToken.getUserId())
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new BadCredentialsException("Account is not active");
        }

        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
        String newRefreshToken = refreshTokenService.createRefreshToken(user);
        String newAccessToken = jwtService.generateAccessToken(user);

        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .expiresIn(jwtService.getExpirationSeconds())
                .build();
    }

    @Transactional
    public void logout(RefreshTokenRequest request) {
        refreshTokenService.revokeRefreshToken(request.getRefreshToken());
    }

    public AuthUserResponse getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || authentication.getPrincipal().equals("anonymousUser")) {
            throw new BadCredentialsException("Not authenticated");
        }

        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new EntityNotFoundException("User not found"));

        return convertToAuthUserResponse(user);
    }

    private AuthUserResponse convertToAuthUserResponse(User user) {
        String fullName = null;
        if (user.getEmployeeId() != null) {
            fullName = employeeRepository.findById(user.getEmployeeId())
                    .map(Employee::getFullName)
                    .orElse(null);
        }

        return AuthUserResponse.builder()
                .id(user.getId())
                .companyId(user.getCompanyId())
                .employeeId(user.getEmployeeId())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .roles(user.getRoles().stream().map(Role::getCode).collect(Collectors.toSet()))
                .fullName(fullName)
                .build();
    }

    private void registerFailedLogin(String login) {
        if (login == null || login.isBlank()) {
            return;
        }
        userRepository.findByEmail(login.trim().toLowerCase()).ifPresent(this::registerFailedLogin);
    }

    private void registerFailedLogin(User user) {
        if (user.getStatus() != UserStatus.ACTIVE) {
            return;
        }
        int attempts = (user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts()) + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_LOGIN_ATTEMPTS) {
            user.setStatus(UserStatus.BLOCKED);
            user.setLockedAt(LocalDateTime.now());
            auditLogService.failure(user, "AUTO_BLOCK_USER_AFTER_FAILED_LOGINS", user.getId(), user.getEmployeeId(), "Too many failed login attempts");
        }
        userRepository.save(user);
    }

    private User createLocalUserFromAd(AdDirectoryService.AdUser adUser) {
        UUID companyId = userRepository.findAll().stream()
                .findFirst()
                .map(User::getCompanyId)
                .orElseThrow(() -> new BadCredentialsException("No local company exists for AD user provisioning"));

        User user = User.builder()
                .companyId(companyId)
                .employeeId(employeeRepository.findFirstByAdUsernameIgnoreCaseOrEmailIgnoreCase(adUser.getUsername(), adUser.getEmail())
                        .map(Employee::getId)
                        .orElse(null))
                .email(adUser.getEmail())
                .passwordHash(passwordEncoder.encode(UUID.randomUUID().toString()))
                .status(UserStatus.ACTIVE)
                .failedLoginAttempts(0)
                .roles(new HashSet<>())
                .build();

        roleRepository.findByCode(adProperties.getDefaultRoleCode())
                .ifPresent(role -> user.getRoles().add(role));

        return userRepository.save(user);
    }
}
