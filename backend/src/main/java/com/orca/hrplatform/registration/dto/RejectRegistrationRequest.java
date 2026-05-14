package com.orca.hrplatform.registration.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectRegistrationRequest {
    @NotBlank
    private String reason;
}
