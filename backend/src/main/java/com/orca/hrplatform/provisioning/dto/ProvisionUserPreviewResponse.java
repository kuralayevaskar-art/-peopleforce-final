package com.orca.hrplatform.provisioning.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ProvisionUserPreviewResponse {
    private String fullName;
    private String login;
    private String corporateEmail;
    private String personalEmail;
    private String department;
    private String temporaryPasswordPreview;
    private boolean dryRun;
    private List<String> synologyFolders;
    private List<String> plannedActions;
}
