package com.orca.hrplatform.provisioning.service;

import com.orca.hrplatform.provisioning.config.ProvisioningProperties;
import com.orca.hrplatform.provisioning.dto.ProvisionUserPreviewResponse;
import com.orca.hrplatform.provisioning.dto.ProvisionUserRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProvisioningPreviewService {
    private final AccountNamingService accountNamingService;
    private final SynologyPathService synologyPathService;
    private final ProvisioningProperties provisioningProperties;

    public ProvisionUserPreviewResponse preview(ProvisionUserRequest request) {
        String login = accountNamingService.generateLogin(request.getFullName());
        String email = accountNamingService.generateEmail(login, provisioningProperties.getDefaultDomain());
        List<String> folders = synologyPathService.employeeFolders(login);

        return ProvisionUserPreviewResponse.builder()
                .fullName(request.getFullName())
                .login(login)
                .corporateEmail(email)
                .personalEmail(request.getPersonalEmail())
                .department(request.getDepartment())
                .temporaryPasswordPreview("Будет сгенерирован при создании и отправлен пользователю один раз")
                .dryRun(provisioningProperties.isDryRun())
                .synologyFolders(folders)
                .plannedActions(List.of(
                        "Create HR employee profile",
                        "Create AD user " + login,
                        "Assign AD groups by department " + request.getDepartment(),
                        "Create Synology folders under " + folders.getFirst(),
                        "Send credentials to " + request.getPersonalEmail()
                ))
                .build();
    }
}
