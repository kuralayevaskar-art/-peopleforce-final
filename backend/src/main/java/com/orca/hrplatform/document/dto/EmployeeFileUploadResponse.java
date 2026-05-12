package com.orca.hrplatform.document.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EmployeeFileUploadResponse {
    private String fileName;
    private String category;
    private String storagePath;
    private String contentType;
    private long size;
}
