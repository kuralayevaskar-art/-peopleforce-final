package com.orca.hrplatform.registration.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RegistrationFileUploadResponse {
    private UUID id;
    private String originalFilename;
    private String storagePath;
    private String contentType;
    private long sizeBytes;
}
