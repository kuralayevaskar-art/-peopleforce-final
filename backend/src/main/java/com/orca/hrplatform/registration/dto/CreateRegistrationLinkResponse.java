package com.orca.hrplatform.registration.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CreateRegistrationLinkResponse {
    private UUID id;
    private String url;
    private LocalDateTime expiresAt;
}
