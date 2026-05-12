package com.orca.hrplatform.integration.synology.controller;

import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.integration.synology.config.SynologyProperties;
import com.orca.hrplatform.integration.synology.dto.SynologySettingsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/integrations/synology")
@RequiredArgsConstructor
public class SynologyIntegrationController {
    private final SynologyProperties synologyProperties;

    @GetMapping("/settings/preview")
    public ApiResponse<SynologySettingsResponse> previewSettings() {
        return ApiResponse.success(SynologySettingsResponse.builder()
                .enabled(synologyProperties.isEnabled())
                .host(synologyProperties.getHost())
                .rootPath(synologyProperties.getRootPath())
                .usernameConfigured(StringUtils.hasText(synologyProperties.getUsername()))
                .passwordConfigured(StringUtils.hasText(synologyProperties.getPassword()))
                .build());
    }

    @PostMapping("/test")
    public ApiResponse<SynologySettingsResponse> testConnection() {
        return ApiResponse.success(previewSettings().getData(), "Synology connection settings are configured for dry-run validation");
    }
}
