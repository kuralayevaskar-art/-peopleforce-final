package com.orca.hrplatform.integration.ad.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdUserMoveRequest {
    @NotBlank
    private String login;

    @NotBlank
    private String department;

    private String managerLogin;
}
