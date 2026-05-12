package com.orca.hrplatform.integration.ad.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdConnectionTestResponse {
    private boolean connected;
    private String url;
    private String baseDn;
    private String message;
}
