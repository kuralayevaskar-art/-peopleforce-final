package com.orca.hrplatform.integration.ad.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdPhotoSyncResponse {
    private String username;
    private String status;
    private String message;
}
