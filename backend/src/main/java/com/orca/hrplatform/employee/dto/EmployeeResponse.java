package com.orca.hrplatform.employee.dto;

import com.orca.hrplatform.employee.entity.Employee;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class EmployeeResponse {
    private UUID id;
    private UUID companyId;
    private UUID departmentId;
    private UUID positionId;
    private String fullName;
    private String firstName;
    private String lastName;
    private String middleName;
    private String email;
    private String phone;
    private String status;
    private String avatarFileId;
    private String adUsername;
    private String zktecoPin;

    public static EmployeeResponse from(Employee employee) {
        return EmployeeResponse.builder()
                .id(employee.getId())
                .companyId(employee.getCompanyId())
                .departmentId(employee.getDepartmentId())
                .positionId(employee.getPositionId())
                .fullName(employee.getFullName())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .middleName(employee.getMiddleName())
                .email(employee.getEmail())
                .phone(employee.getPhone())
                .status(employee.getStatus() != null ? employee.getStatus().name() : null)
                .avatarFileId(employee.getAvatarFileId())
                .adUsername(employee.getAdUsername())
                .zktecoPin(employee.getZktecoPin())
                .build();
    }
}
