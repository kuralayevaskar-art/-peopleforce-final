package com.orca.hrplatform.integration.ad.controller;

import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.integration.ad.config.AdProperties;
import com.orca.hrplatform.integration.ad.dto.AdConnectionTestResponse;
import com.orca.hrplatform.integration.ad.dto.AdSettingsRequest;
import com.orca.hrplatform.integration.ad.dto.AdSettingsResponse;
import com.orca.hrplatform.integration.ad.dto.AdUserResponse;
import com.orca.hrplatform.integration.ad.service.AdDirectoryService;
import com.orca.hrplatform.integration.ad.service.AdEmployeeSyncService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/admin/integrations/ad")
@RequiredArgsConstructor
public class AdIntegrationController {

    private final AdProperties adProperties;
    private final AdDirectoryService adDirectoryService;
    private final AdEmployeeSyncService adEmployeeSyncService;

    @GetMapping("/settings/preview")
    public ApiResponse<AdSettingsResponse> previewSettings() {
        return ApiResponse.success(AdSettingsResponse.builder()
                .enabled(adProperties.isEnabled())
                .url(adProperties.getUrl())
                .baseDn(adProperties.getBaseDn())
                .bindDn(adProperties.getBindDn())
                .userFilter(adProperties.getUserFilter())
                .groupFilter(adProperties.getGroupFilter())
                .searchBaseDns(adProperties.getSearchBaseDns())
                .build());
    }

    @PostMapping("/test")
    public ApiResponse<AdConnectionTestResponse> testConnection(@Valid @RequestBody AdSettingsRequest request) {
        return ApiResponse.success(adDirectoryService.test(request));
    }

    @PutMapping("/settings")
    public ApiResponse<AdSettingsResponse> updateSettings(@Valid @RequestBody AdSettingsRequest request) {
        adProperties.setEnabled(true);
        adProperties.setUrl(request.getUrl());
        adProperties.setBaseDn(request.getBaseDn());
        adProperties.setBindDn(request.getBindDn());
        if (request.getBindPassword() != null && !request.getBindPassword().isBlank()) {
            adProperties.setBindPassword(request.getBindPassword());
        }
        adProperties.setUserFilter(request.getUserFilter());
        adProperties.setGroupFilter(request.getGroupFilter());
        adProperties.setSearchBaseDns(request.getSearchBaseDns());

        return previewSettings();
    }

    @PostMapping("/users/preview")
    public ApiResponse<List<AdUserResponse>> previewUsers(@Valid @RequestBody AdSettingsRequest request) {
        return ApiResponse.success(toResponses(adDirectoryService.listUsers(request)), "AD users loaded");
    }

    @PostMapping("/users/sync")
    public ApiResponse<List<AdUserResponse>> syncUsers(@Valid @RequestBody AdSettingsRequest request) {
        List<AdDirectoryService.AdUser> users = adDirectoryService.listUsers(request);
        adEmployeeSyncService.sync(users);
        return ApiResponse.success(toResponses(users), "AD users synchronized");
    }

    private List<AdUserResponse> toResponses(List<AdDirectoryService.AdUser> users) {
        return users.stream()
                .map(user -> AdUserResponse.builder()
                        .username(user.getUsername())
                        .displayName(user.getDisplayName())
                        .email(user.getEmail())
                        .department(user.getDepartment())
                        .title(user.getTitle())
                        .manager(user.getManager())
                        .dn(user.getDn())
                        .build())
                .toList();
    }
}
