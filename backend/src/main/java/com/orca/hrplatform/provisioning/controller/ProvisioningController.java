package com.orca.hrplatform.provisioning.controller;

import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.provisioning.dto.ProvisionUserPreviewResponse;
import com.orca.hrplatform.provisioning.dto.ProvisionUserRequest;
import com.orca.hrplatform.provisioning.service.ProvisioningPreviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/provisioning")
@RequiredArgsConstructor
public class ProvisioningController {
    private final ProvisioningPreviewService provisioningPreviewService;

    @PostMapping("/users/preview")
    public ApiResponse<ProvisionUserPreviewResponse> previewUser(@Valid @RequestBody ProvisionUserRequest request) {
        return ApiResponse.success(provisioningPreviewService.preview(request), "Provisioning preview generated");
    }
}
