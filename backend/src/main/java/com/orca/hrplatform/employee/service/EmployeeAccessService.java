package com.orca.hrplatform.employee.service;

import com.orca.hrplatform.audit.service.AuditLogService;
import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.common.security.SecurityUtils;
import com.orca.hrplatform.employee.dto.EmployeeResponse;
import com.orca.hrplatform.employee.dto.EmployeeUpdateRequest;
import com.orca.hrplatform.employee.entity.Employee;
import com.orca.hrplatform.employee.entity.EmployeeStatus;
import com.orca.hrplatform.employee.repository.EmployeeRepository;
import com.orca.hrplatform.provisioning.service.PowerShellProvisioningService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmployeeAccessService {
    private final EmployeeRepository employeeRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final PowerShellProvisioningService powerShellProvisioningService;

    public List<EmployeeResponse> listVisible() {
        User current = currentUser();
        if (isAdmin(current)) {
            return employeeRepository.findByCompanyId(current.getCompanyId()).stream()
                    .map(EmployeeResponse::from)
                    .toList();
        }
        if (current.getEmployeeId() == null) {
            return List.of();
        }
        return employeeRepository.findById(current.getEmployeeId()).stream()
                .map(EmployeeResponse::from)
                .toList();
    }

    public EmployeeResponse get(UUID id) {
        User current = currentUser();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        requireCanAccess(current, employee);
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse update(UUID id, EmployeeUpdateRequest request) {
        User current = currentUser();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Employee not found"));
        requireCanAccess(current, employee);

        String oldValue = "phone=" + employee.getPhone() + ", email=" + employee.getEmail()
                + ", departmentId=" + employee.getDepartmentId() + ", positionId=" + employee.getPositionId();

        if (StringUtils.hasText(request.getPhone())) {
            employee.setPhone(request.getPhone());
        }
        if (StringUtils.hasText(request.getPersonalEmail())) {
            employee.setEmail(request.getPersonalEmail());
        }
        if (StringUtils.hasText(request.getAvatarFileId())) {
            employee.setAvatarFileId(request.getAvatarFileId());
        }

        if (isAdmin(current)) {
            if (request.getDepartmentId() != null) {
                employee.setDepartmentId(request.getDepartmentId());
            }
            if (request.getPositionId() != null) {
                employee.setPositionId(request.getPositionId());
            }
            if (StringUtils.hasText(request.getStatus())) {
                employee.setStatus(EmployeeStatus.valueOf(request.getStatus()));
            }
        }

        Employee saved = employeeRepository.save(employee);
        String newValue = "phone=" + saved.getPhone() + ", email=" + saved.getEmail()
                + ", departmentId=" + saved.getDepartmentId() + ", positionId=" + saved.getPositionId();
        auditLogService.success(current, "UPDATE_EMPLOYEE", null, saved.getId(), oldValue, newValue);
        syncAdContactIfNeeded(current, saved, request);
        return EmployeeResponse.from(saved);
    }

    private void syncAdContactIfNeeded(User current, Employee employee, EmployeeUpdateRequest request) {
        if (!isAdmin(current) || !StringUtils.hasText(employee.getAdUsername()) || !StringUtils.hasText(request.getPhone())) {
            return;
        }
        try {
            PowerShellProvisioningService.ProvisioningCommandResult result =
                    powerShellProvisioningService.updateAdUserContact(PowerShellProvisioningService.UpdateAdUserContactCommand.builder()
                            .login(employee.getAdUsername())
                            .phone(employee.getPhone())
                            .build());
            auditLogService.success(current, "UPDATE_AD_USER_CONTACT", null, employee.getId(), null, result.getStatus() + ": " + result.getMessage());
        } catch (RuntimeException ex) {
            auditLogService.failure(current, "UPDATE_AD_USER_CONTACT", null, employee.getId(), ex.getMessage());
        }
    }

    private void requireCanAccess(User current, Employee employee) {
        if (!current.getCompanyId().equals(employee.getCompanyId())) {
            throw new AccessDeniedException("Employee belongs to another company");
        }
        if (!isAdmin(current) && (current.getEmployeeId() == null || !current.getEmployeeId().equals(employee.getId()))) {
            throw new AccessDeniedException("You can edit only your own employee profile");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream().anyMatch(role -> List.of("SUPER_ADMIN", "HR_ADMIN", "INTEGRATION_ADMIN").contains(role.getCode()));
    }

    private User currentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
    }
}
