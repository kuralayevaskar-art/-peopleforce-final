package com.orca.hrplatform.integration.zkteco.controller;

import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.integration.zkteco.config.ZktecoProperties;
import com.orca.hrplatform.integration.zkteco.dto.ZktecoSettingsResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/integrations/zkteco")
@RequiredArgsConstructor
public class ZktecoIntegrationController {
    private final ZktecoProperties zktecoProperties;

    @GetMapping("/settings/preview")
    public ApiResponse<ZktecoSettingsResponse> previewSettings() {
        return ApiResponse.success(ZktecoSettingsResponse.builder()
                .enabled(zktecoProperties.isEnabled())
                .host(zktecoProperties.getHost())
                .databaseName(zktecoProperties.getDatabaseName())
                .usernameConfigured(StringUtils.hasText(zktecoProperties.getUsername()))
                .passwordConfigured(StringUtils.hasText(zktecoProperties.getPassword()))
                .build());
    }

    @PostMapping("/test")
    public ApiResponse<ZktecoSettingsResponse> testConnection() {
        return ApiResponse.success(previewSettings().getData(), "ZKTeco connection settings are configured for dry-run validation");
    }
}
