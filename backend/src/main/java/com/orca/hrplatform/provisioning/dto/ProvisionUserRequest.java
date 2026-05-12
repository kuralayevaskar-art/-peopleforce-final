package com.orca.hrplatform.provisioning.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ProvisionUserRequest {
    @NotBlank
    private String fullName;

    @NotBlank
    private String department;

    @Email
    @NotBlank
    private String personalEmail;
}
