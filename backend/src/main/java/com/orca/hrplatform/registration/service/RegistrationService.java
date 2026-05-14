package com.orca.hrplatform.registration.service;

import com.orca.hrplatform.audit.service.AuditLogService;
import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.common.security.SecurityUtils;
import com.orca.hrplatform.document.entity.FileMetadata;
import com.orca.hrplatform.document.repository.FileMetadataRepository;
import com.orca.hrplatform.employee.repository.EmployeeRepository;
import com.orca.hrplatform.integration.synology.config.SynologyProperties;
import com.orca.hrplatform.provisioning.config.ProvisioningProperties;
import com.orca.hrplatform.provisioning.service.AccountNamingService;
import com.orca.hrplatform.provisioning.service.PowerShellProvisioningService;
import com.orca.hrplatform.provisioning.service.TemporaryPasswordService;
import com.orca.hrplatform.integration.zkteco.service.ZktecoFacePhotoService;
import com.orca.hrplatform.mail.service.CredentialMailService;
import com.orca.hrplatform.registration.dto.CreateRegistrationLinkResponse;
import com.orca.hrplatform.registration.dto.RegistrationFileUploadResponse;
import com.orca.hrplatform.registration.dto.RegistrationSubmitRequest;
import com.orca.hrplatform.registration.entity.RegistrationLink;
import com.orca.hrplatform.registration.entity.RegistrationRequest;
import com.orca.hrplatform.registration.repository.RegistrationLinkRepository;
import com.orca.hrplatform.registration.repository.RegistrationRequestRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RegistrationLinkRepository linkRepository;
    private final RegistrationRequestRepository requestRepository;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final SynologyProperties synologyProperties;
    private final AccountNamingService accountNamingService;
    private final ProvisioningProperties provisioningProperties;
    private final AuditLogService auditLogService;
    private final TemporaryPasswordService temporaryPasswordService;
    private final PowerShellProvisioningService powerShellProvisioningService;
    private final ZktecoFacePhotoService zktecoFacePhotoService;
    private final CredentialMailService credentialMailService;

    @Transactional
    public CreateRegistrationLinkResponse createLink(String origin) {
        User current = currentUser();
        String token = newToken();
        RegistrationLink link = linkRepository.save(RegistrationLink.builder()
                .companyId(current.getCompanyId())
                .tokenHash(hash(token))
                .status("ACTIVE")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .createdBy(current.getId())
                .build());
        auditLogService.success(current, "CREATE_REGISTRATION_LINK", null, null, null, link.getId().toString());
        String baseUrl = origin != null && !origin.isBlank() ? origin : "";
        return CreateRegistrationLinkResponse.builder()
                .id(link.getId())
                .url(baseUrl + "/registration/" + token)
                .expiresAt(link.getExpiresAt())
                .build();
    }

    public RegistrationLink validate(String token) {
        RegistrationLink link = linkRepository.findByTokenHash(hash(token))
                .orElseThrow(() -> new EntityNotFoundException("Ссылка недействительна или срок ее действия истек. Обратитесь к администратору."));
        if (!"ACTIVE".equals(link.getStatus()) || link.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new EntityNotFoundException("Ссылка недействительна или срок ее действия истек. Обратитесь к администратору.");
        }
        return link;
    }

    @Transactional
    public RegistrationFileUploadResponse uploadFile(String token, String type, MultipartFile file) {
        RegistrationLink link = validate(token);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is required");
        }
        String safeType = switch (type == null ? "" : type.toLowerCase()) {
            case "identity", "face" -> type.toLowerCase();
            default -> throw new IllegalArgumentException("Unsupported registration file type");
        };
        validateFileType(safeType, file);

        String safeFileName = sanitizeFileName(file.getOriginalFilename());
        Path folder = Path.of(synologyProperties.getRootPath(), "_registration", link.getId().toString(), safeType).normalize();
        Path target = folder.resolve(safeFileName).normalize();
        if (!target.startsWith(folder)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        try {
            Files.createDirectories(folder);
            file.transferTo(target);
        } catch (IOException ex) {
            log.warn("Cannot save registration file to primary storage {}, trying local fallback", folder, ex);
            folder = Path.of("C:/people2/finesh/storage/people", "_registration", link.getId().toString(), safeType).normalize();
            target = folder.resolve(safeFileName).normalize();
            if (!target.startsWith(folder)) {
                throw new IllegalArgumentException("Invalid file path");
            }
            try {
                Files.createDirectories(folder);
                file.transferTo(target);
            } catch (IOException fallbackEx) {
                throw new IllegalStateException("Cannot save registration file", fallbackEx);
            }
        }

        FileMetadata metadata = fileMetadataRepository.save(FileMetadata.builder()
                .companyId(link.getCompanyId())
                .originalFilename(safeFileName)
                .storagePath(target.toString())
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .uploadedBy(null)
                .build());

        return RegistrationFileUploadResponse.builder()
                .id(metadata.getId())
                .originalFilename(metadata.getOriginalFilename())
                .storagePath(metadata.getStoragePath())
                .contentType(metadata.getContentType())
                .sizeBytes(metadata.getSizeBytes() == null ? 0 : metadata.getSizeBytes())
                .build();
    }

    @Transactional
    public RegistrationRequest submit(String token, RegistrationSubmitRequest request) {
        RegistrationLink link = validate(token);
        String firstName = request.getFirstName().trim();
        String lastName = request.getLastName().trim();
        String fullName = (firstName + " " + lastName).trim();
        RegistrationRequest created = requestRepository.save(RegistrationRequest.builder()
                .companyId(link.getCompanyId())
                .linkId(link.getId())
                .fullName(fullName)
                .firstName(firstName)
                .lastName(lastName)
                .department(request.getDepartment())
                .phone(request.getPhone())
                .personalEmail(request.getPersonalEmail())
                .identityDocumentFileId(request.getIdentityDocumentFileId())
                .facePhotoFileId(request.getFacePhotoFileId())
                .status("PENDING")
                .build());
        link.setStatus("USED");
        link.setUsedAt(LocalDateTime.now());
        linkRepository.save(link);
        return created;
    }

    public List<RegistrationRequest> listCurrentCompanyRequests() {
        return requestRepository.findByCompanyIdOrderByCreatedAtDesc(currentUser().getCompanyId());
    }

    @Transactional
    public RegistrationRequest approve(UUID id) {
        return approve(id, null);
    }

    @Transactional
    public RegistrationRequest approve(UUID id, String departmentOverride) {
        User current = currentUser();
        RegistrationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registration request not found"));
        if (!canApprove(request.getStatus())) {
            throw new IllegalArgumentException("Only pending or failed registration requests can be approved");
        }
        String login = accountNamingService.generateLogin(request.getFirstName(), request.getLastName());
        String corporateEmail = accountNamingService.generateEmail(login, provisioningProperties.getDefaultDomain());
        String temporaryPassword = temporaryPasswordService.generate();
        String description = registrationDescription();
        request.setCorporateEmail(corporateEmail);
        request.setRejectionReason(null);
        if (StringUtils.hasText(departmentOverride)) {
            request.setDepartment(departmentOverride.trim());
        }
        if (!StringUtils.hasText(request.getDepartment())) {
            throw new IllegalArgumentException("Department is required before approval");
        }

        if (userRepository.findByEmail(corporateEmail).isPresent()
                || employeeRepository.findFirstByAdUsernameIgnoreCaseOrEmailIgnoreCase(login, corporateEmail).isPresent()) {
            request.setStatus("DUPLICATE_FAILED");
            request.setRejectionReason("User already exists: " + corporateEmail);
            auditLogService.failure(current, "APPROVE_REGISTRATION_DUPLICATE", null, null, "User already exists: " + corporateEmail);
            return requestRepository.save(request);
        }

        try {
            powerShellProvisioningService.createAdUser(PowerShellProvisioningService.CreateAdUserCommand.builder()
                    .fullName(request.getFullName())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .login(login)
                    .corporateEmail(corporateEmail)
                    .description(description)
                    .office("De Montfort University Kazakhstan")
                    .phone(request.getPhone())
                    .webPage("https://dmuk.edu.kz/")
                    .department(request.getDepartment())
                    .temporaryPassword(temporaryPassword)
                    .build());
            auditLogService.success(current, "CREATE_AD_USER", null, null, null, corporateEmail);
        } catch (RuntimeException ex) {
            request.setStatus("AD_CREATE_FAILED");
            request.setRejectionReason(ex.getMessage());
            auditLogService.failure(current, "CREATE_AD_USER", null, null, ex.getMessage());
            return requestRepository.save(request);
        }

        try {
            powerShellProvisioningService.startM365DeltaSync();
            auditLogService.success(current, "START_M365_SYNC", null, null, null, corporateEmail);
        } catch (RuntimeException ex) {
            request.setStatus("M365_SYNC_FAILED");
            request.setRejectionReason(ex.getMessage());
            auditLogService.failure(current, "START_M365_SYNC", null, null, ex.getMessage());
            return requestRepository.save(request);
        }

        try {
            ZktecoFacePhotoService.ZktecoPhotoUploadResult zktResult =
                    zktecoFacePhotoService.uploadFacePhoto(login, request.getFirstName(), request.getLastName(), request.getFacePhotoFileId());
            auditLogService.success(current, "UPLOAD_ZKT_FACE_PHOTO", null, null, null, zktResult.getStatus() + ": " + zktResult.getMessage());
            if ("SKIPPED".equals(zktResult.getStatus()) && zktResult.getMessage() != null && zktResult.getMessage().contains("not synced from AD yet")) {
                request.setStatus("ZKT_PHOTO_PENDING");
            }
        } catch (RuntimeException ex) {
            request.setStatus("ZKT_PHOTO_FAILED");
            request.setRejectionReason(ex.getMessage());
            auditLogService.failure(current, "UPLOAD_ZKT_FACE_PHOTO", null, null, ex.getMessage());
            return requestRepository.save(request);
        }

        try {
            credentialMailService.sendCredentials(request.getPersonalEmail(), corporateEmail, temporaryPassword);
            auditLogService.success(current, "SEND_CREDENTIAL_EMAIL", null, null, null, request.getPersonalEmail());
        } catch (RuntimeException ex) {
            request.setStatus("EMAIL_FAILED");
            request.setRejectionReason(ex.getMessage());
            auditLogService.failure(current, "SEND_CREDENTIAL_EMAIL", null, null, ex.getMessage());
            return requestRepository.save(request);
        }

        if (!"ZKT_PHOTO_PENDING".equals(request.getStatus())) {
            request.setStatus("COMPLETED");
        }
        request.setApprovedBy(current.getId());
        request.setApprovedAt(LocalDateTime.now());
        auditLogService.success(current, "APPROVE_REGISTRATION_REQUEST", null, null, null, id.toString());
        return requestRepository.save(request);
    }

    private boolean canApprove(String status) {
        return "PENDING".equals(status)
                || "AD_CREATE_FAILED".equals(status)
                || "DUPLICATE_FAILED".equals(status)
                || "M365_SYNC_FAILED".equals(status)
                || "ZKT_PHOTO_FAILED".equals(status)
                || "EMAIL_FAILED".equals(status);
    }

    private String registrationDescription() {
        LocalDate today = LocalDate.now();
        long registrationsToday = requestRepository.countByCreatedAtBetween(today.atStartOfDay(), today.plusDays(1).atStartOfDay());
        long offset = Math.max(0, registrationsToday - 1);
        return today.plusDays(offset).format(DateTimeFormatter.BASIC_ISO_DATE);
    }

    @Scheduled(fixedDelayString = "${app.registration.zkt-photo-retry-ms:300000}")
    @Transactional
    public void retryPendingZktPhotos() {
        for (RegistrationRequest request : requestRepository.findTop50ByStatusOrderByCreatedAtAsc("ZKT_PHOTO_PENDING")) {
            if (!StringUtils.hasText(request.getCorporateEmail()) || !StringUtils.hasText(request.getFacePhotoFileId())) {
                continue;
            }
            String login = request.getCorporateEmail().contains("@")
                    ? request.getCorporateEmail().substring(0, request.getCorporateEmail().indexOf('@'))
                    : request.getCorporateEmail();
            try {
                ZktecoFacePhotoService.ZktecoPhotoUploadResult result =
                        zktecoFacePhotoService.uploadFacePhoto(login, request.getFirstName(), request.getLastName(), request.getFacePhotoFileId());
                auditLogService.success(null, "RETRY_ZKT_FACE_PHOTO", null, null, null, result.getStatus() + ": " + result.getMessage());
                if ("SUCCESS".equals(result.getStatus())) {
                    request.setStatus("COMPLETED");
                    requestRepository.save(request);
                }
            } catch (RuntimeException ex) {
                log.warn("ZKT Face ID retry failed for registration request {}: {}", request.getId(), ex.getMessage());
                auditLogService.failure(null, "RETRY_ZKT_FACE_PHOTO", null, null, ex.getMessage());
            }
        }
    }

    @Transactional
    public RegistrationRequest reject(UUID id, String reason) {
        User current = currentUser();
        RegistrationRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Registration request not found"));
        String rejectionText = StringUtils.hasText(reason) ? reason.trim() : "Отклонено администратором";
        request.setStatus("REJECTED");
        request.setRejectionReason(rejectionText);
        request.setRejectedBy(current.getId());
        request.setRejectedAt(LocalDateTime.now());
        try {
            credentialMailService.sendRegistrationRejected(request.getPersonalEmail(), request.getFullName(), rejectionText);
            auditLogService.success(current, "SEND_REGISTRATION_REJECTED_EMAIL", null, null, null, request.getPersonalEmail());
        } catch (RuntimeException ex) {
            request.setStatus("REJECT_EMAIL_FAILED");
            request.setRejectionReason(rejectionText + " / Email failed: " + ex.getMessage());
            auditLogService.failure(current, "SEND_REGISTRATION_REJECTED_EMAIL", null, null, ex.getMessage());
            return requestRepository.save(request);
        }
        auditLogService.success(current, "REJECT_REGISTRATION_REQUEST", null, null, null, rejectionText);
        return requestRepository.save(request);
    }

    private User currentUser() {
        return userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
    }

    private String newToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot hash registration token", ex);
        }
    }

    private void validateFileType(String type, MultipartFile file) {
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if ("face".equals(type)) {
            boolean image = contentType.startsWith("image/") || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png");
            if (!image) {
                throw new IllegalArgumentException("Face ID photo must be an image");
            }
        }
        if ("identity".equals(type)) {
            boolean document = contentType.startsWith("image/") || "application/pdf".equals(contentType)
                    || name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".pdf");
            if (!document) {
                throw new IllegalArgumentException("Identity document must be an image or PDF");
            }
        }
        if (file.getSize() > 50L * 1024 * 1024) {
            throw new IllegalArgumentException("File is too large. Maximum size is 50 MB");
        }
    }

    private String sanitizeFileName(String fileName) {
        String value = StringUtils.hasText(fileName) ? fileName : "registration-file";
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
