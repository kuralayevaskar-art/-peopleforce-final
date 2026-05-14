package com.orca.hrplatform.employee.dto;

import jakarta.validation.constraints.Email;
import lombok.Data;

import java.util.UUID;

@Data
public class EmployeeUpdateRequest {
    private String phone;
    @Email
    private String personalEmail;
    private String avatarFileId;
    private UUID departmentId;
    private UUID positionId;
    private String status;
}
