package com.orca.hrplatform.provisioning.service;

import com.orca.hrplatform.provisioning.config.ProvisioningProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PowerShellProvisioningService {
    private final ProvisioningProperties properties;

    public ProvisioningCommandResult createAdUser(CreateAdUserCommand command) {
        if (properties.isDryRun()) {
            return ProvisioningCommandResult.skipped("PROVISIONING_DRY_RUN=true, AD user was not created");
        }
        if (!StringUtils.hasText(properties.getAdTargetOu())) {
            throw new IllegalStateException("AD target OU is not configured");
        }

        String script = """
                Import-Module ActiveDirectory;
                $password = ConvertTo-SecureString $env:DMUK_NEW_USER_PASSWORD -AsPlainText -Force;
                $changePasswordAtLogon = $env:DMUK_NEW_USER_CHANGE_PASSWORD_AT_LOGON -eq 'true';
                $existing = Get-ADUser -Identity $env:DMUK_NEW_USER_LOGIN -Properties UserPrincipalName -ErrorAction SilentlyContinue;
                if (-not $existing) {
                  $existing = Get-ADUser -Filter "UserPrincipalName -eq '$($env:DMUK_NEW_USER_EMAIL)'" -Properties UserPrincipalName -ErrorAction SilentlyContinue | Select-Object -First 1;
                }
                if ($existing) {
                  $identity = $existing.DistinguishedName;
                  Set-ADUser `
                    -Identity $identity `
                    -DisplayName $env:DMUK_NEW_USER_FULL_NAME `
                    -GivenName $env:DMUK_NEW_USER_FIRST_NAME `
                    -Surname $env:DMUK_NEW_USER_LAST_NAME `
                    -UserPrincipalName $env:DMUK_NEW_USER_EMAIL `
                    -EmailAddress $env:DMUK_NEW_USER_EMAIL `
                    -Description $env:DMUK_NEW_USER_DESCRIPTION `
                    -Office $env:DMUK_NEW_USER_OFFICE `
                    -OfficePhone $env:DMUK_NEW_USER_PHONE `
                    -MobilePhone $env:DMUK_NEW_USER_PHONE `
                    -HomePage $env:DMUK_NEW_USER_WEB_PAGE `
                    -Department $env:DMUK_NEW_USER_DEPARTMENT;
                  Set-ADAccountPassword -Identity $identity -Reset -NewPassword $password;
                  Set-ADUser -Identity $identity -ChangePasswordAtLogon $changePasswordAtLogon;
                  Enable-ADAccount -Identity $identity;
                  Write-Output "Existing AD user updated and password reset.";
                } else {
                  New-ADUser `
                    -Name $env:DMUK_NEW_USER_FULL_NAME `
                    -DisplayName $env:DMUK_NEW_USER_FULL_NAME `
                    -GivenName $env:DMUK_NEW_USER_FIRST_NAME `
                    -Surname $env:DMUK_NEW_USER_LAST_NAME `
                    -SamAccountName $env:DMUK_NEW_USER_LOGIN `
                    -UserPrincipalName $env:DMUK_NEW_USER_EMAIL `
                    -EmailAddress $env:DMUK_NEW_USER_EMAIL `
                    -Description $env:DMUK_NEW_USER_DESCRIPTION `
                    -Office $env:DMUK_NEW_USER_OFFICE `
                    -OfficePhone $env:DMUK_NEW_USER_PHONE `
                    -MobilePhone $env:DMUK_NEW_USER_PHONE `
                    -HomePage $env:DMUK_NEW_USER_WEB_PAGE `
                    -Department $env:DMUK_NEW_USER_DEPARTMENT `
                    -Path $env:DMUK_AD_TARGET_OU `
                    -AccountPassword $password `
                    -Enabled $true `
                    -ChangePasswordAtLogon $changePasswordAtLogon;
                  Write-Output "New AD user created.";
                }
                """;

        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("DMUK_NEW_USER_PASSWORD", safe(command.getTemporaryPassword()));
        environment.put("DMUK_NEW_USER_CHANGE_PASSWORD_AT_LOGON", Boolean.toString(properties.isChangePasswordAtLogon()));
        environment.put("DMUK_NEW_USER_FULL_NAME", safe(command.getFullName()));
        environment.put("DMUK_NEW_USER_FIRST_NAME", safe(command.getFirstName()));
        environment.put("DMUK_NEW_USER_LAST_NAME", safe(command.getLastName()));
        environment.put("DMUK_NEW_USER_LOGIN", safe(command.getLogin()));
        environment.put("DMUK_NEW_USER_EMAIL", safe(command.getCorporateEmail()));
        environment.put("DMUK_NEW_USER_DESCRIPTION", safe(command.getDescription()));
        environment.put("DMUK_NEW_USER_OFFICE", safe(command.getOffice()));
        environment.put("DMUK_NEW_USER_PHONE", safe(command.getPhone()));
        environment.put("DMUK_NEW_USER_WEB_PAGE", safe(command.getWebPage()));
        environment.put("DMUK_NEW_USER_DEPARTMENT", safe(command.getDepartment()));
        environment.put("DMUK_AD_TARGET_OU", safe(properties.getAdTargetOu()));
        return run(script, environment);
    }

    public ProvisioningCommandResult startM365DeltaSync() {
        if (properties.isDryRun()) {
            return ProvisioningCommandResult.skipped("PROVISIONING_DRY_RUN=true, Microsoft 365 sync was not started");
        }
        if (!properties.isM365SyncEnabled()) {
            return ProvisioningCommandResult.skipped("Microsoft 365 sync is disabled");
        }
        if (StringUtils.hasText(properties.getM365SyncHost())) {
            String script = StringUtils.hasText(properties.getM365SyncUsername()) && StringUtils.hasText(properties.getM365SyncPassword())
                    ? """
                    $securePassword = ConvertTo-SecureString $env:DMUK_M365_SYNC_PASSWORD -AsPlainText -Force;
                    $credential = New-Object System.Management.Automation.PSCredential($env:DMUK_M365_SYNC_USERNAME, $securePassword);
                    Invoke-Command -ComputerName $env:DMUK_M365_SYNC_HOST -Credential $credential -ScriptBlock {
                      Import-Module ADSync;
                      Start-ADSyncSyncCycle -PolicyType Delta;
                    }
                    """
                    : """
                    Invoke-Command -ComputerName $env:DMUK_M365_SYNC_HOST -ScriptBlock {
                      Import-Module ADSync;
                      Start-ADSyncSyncCycle -PolicyType Delta;
                    }
                    """;
            Map<String, String> environment = new LinkedHashMap<>();
            environment.put("DMUK_M365_SYNC_HOST", safe(properties.getM365SyncHost()));
            environment.put("DMUK_M365_SYNC_USERNAME", safe(properties.getM365SyncUsername()));
            environment.put("DMUK_M365_SYNC_PASSWORD", safe(properties.getM365SyncPassword()));
            return run(script, environment);
        }
        return run("Import-Module ADSync; Start-ADSyncSyncCycle -PolicyType Delta", Map.of());
    }

    public ProvisioningCommandResult updateAdUserContact(UpdateAdUserContactCommand command) {
        if (properties.isDryRun()) {
            return ProvisioningCommandResult.skipped("PROVISIONING_DRY_RUN=true, AD user was not updated");
        }
        if (!StringUtils.hasText(command.getLogin())) {
            throw new IllegalArgumentException("AD username is required");
        }

        String script = """
                Import-Module ActiveDirectory;
                $updates = @{};
                if ($env:DMUK_AD_USER_PHONE) { $updates['MobilePhone'] = $env:DMUK_AD_USER_PHONE; }
                if ($updates.Count -gt 0) {
                  Set-ADUser -Identity $env:DMUK_AD_USER_LOGIN @updates;
                }
                """;

        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("DMUK_AD_USER_LOGIN", safe(command.getLogin()));
        environment.put("DMUK_AD_USER_PHONE", safe(command.getPhone()));
        return run(script, environment);
    }

    public ProvisioningCommandResult updateAdUserDepartment(UpdateAdUserDepartmentCommand command) {
        if (properties.isDryRun()) {
            return ProvisioningCommandResult.skipped("PROVISIONING_DRY_RUN=true, AD department and manager were not updated");
        }
        if (!StringUtils.hasText(command.getLogin())) {
            throw new IllegalArgumentException("AD username is required");
        }
        if (!StringUtils.hasText(command.getDepartment())) {
            throw new IllegalArgumentException("AD department is required");
        }

        String script = """
                Import-Module ActiveDirectory;
                $updates = @{ Department = $env:DMUK_AD_USER_DEPARTMENT };
                if ($env:DMUK_AD_MANAGER_LOGIN) {
                  $manager = Get-ADUser -Identity $env:DMUK_AD_MANAGER_LOGIN -Properties DistinguishedName;
                  $updates['Manager'] = $manager.DistinguishedName;
                }
                Set-ADUser -Identity $env:DMUK_AD_USER_LOGIN @updates;
                if (-not $env:DMUK_AD_MANAGER_LOGIN) {
                  Set-ADUser -Identity $env:DMUK_AD_USER_LOGIN -Clear Manager;
                }
                """;

        Map<String, String> environment = new LinkedHashMap<>();
        environment.put("DMUK_AD_USER_LOGIN", safe(command.getLogin()));
        environment.put("DMUK_AD_USER_DEPARTMENT", safe(command.getDepartment()));
        environment.put("DMUK_AD_MANAGER_LOGIN", safe(command.getManagerLogin()));
        return run(script, environment);
    }

    private ProvisioningCommandResult run(String script, Map<String, String> environment) {
        try {
            List<String> command = List.of(properties.getPowershellExecutable(), "-NoProfile", "-NonInteractive", "-ExecutionPolicy", "Bypass", "-Command", script);
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.environment().putAll(environment);
            Process process = builder.start();

            boolean finished = process.waitFor(Duration.ofMinutes(2).toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                throw new IllegalStateException("PowerShell command timeout");
            }

            String output = read(process.inputReader(StandardCharsets.UTF_8));
            String error = read(process.errorReader(StandardCharsets.UTF_8));
            if (process.exitValue() != 0) {
                throw new IllegalStateException(sanitize(error.isBlank() ? output : error));
            }
            return ProvisioningCommandResult.success(sanitize(output));
        } catch (Exception ex) {
            throw new IllegalStateException("PowerShell command failed: " + sanitize(ex.getMessage()), ex);
        }
    }

    private String read(BufferedReader reader) throws Exception {
        List<String> lines = new ArrayList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }
        return String.join("\n", lines);
    }

    private String sanitize(String value) {
        return value == null ? "" : value.replaceAll("(?i)password\\s*[:=]\\s*\\S+", "password=***");
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    @Data
    @Builder
    public static class CreateAdUserCommand {
        private String fullName;
        private String firstName;
        private String lastName;
        private String login;
        private String corporateEmail;
        private String description;
        private String office;
        private String phone;
        private String webPage;
        private String department;
        private String temporaryPassword;
    }

    @Data
    @Builder
    public static class UpdateAdUserContactCommand {
        private String login;
        private String phone;
    }

    @Data
    @Builder
    public static class UpdateAdUserDepartmentCommand {
        private String login;
        private String department;
        private String managerLogin;
    }

    @Data
    @Builder
    public static class ProvisioningCommandResult {
        private String status;
        private String message;

        public static ProvisioningCommandResult success(String message) {
            return ProvisioningCommandResult.builder().status("SUCCESS").message(message).build();
        }

        public static ProvisioningCommandResult skipped(String message) {
            return ProvisioningCommandResult.builder().status("SKIPPED").message(message).build();
        }

        public static ProvisioningCommandResult error(String message) {
            return ProvisioningCommandResult.builder().status("ERROR").message(message).build();
        }
    }
}
