package com.orca.hrplatform.registration.controller;

import com.orca.hrplatform.audit.service.AuditLogService;
import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.common.security.SecurityUtils;
import com.orca.hrplatform.document.entity.FileMetadata;
import com.orca.hrplatform.document.repository.FileMetadataRepository;
import com.orca.hrplatform.registration.dto.ApproveRegistrationRequest;
import com.orca.hrplatform.registration.dto.CreateRegistrationLinkResponse;
import com.orca.hrplatform.registration.dto.RegistrationFileUploadResponse;
import com.orca.hrplatform.registration.dto.RegistrationSubmitRequest;
import com.orca.hrplatform.registration.dto.RejectRegistrationRequest;
import com.orca.hrplatform.registration.entity.RegistrationLink;
import com.orca.hrplatform.registration.entity.RegistrationRequest;
import com.orca.hrplatform.registration.service.RegistrationService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class RegistrationController {
    private final RegistrationService registrationService;
    private final FileMetadataRepository fileMetadataRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @PostMapping("/admin/registration-links")
    public ApiResponse<CreateRegistrationLinkResponse> createLink(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        return ApiResponse.success(registrationService.createLink(origin), "Registration link created");
    }

    @GetMapping("/admin/registration-requests")
    public ApiResponse<List<RegistrationRequest>> listRequests() {
        return ApiResponse.success(registrationService.listCurrentCompanyRequests());
    }

    @GetMapping("/admin/registration-files/{id}")
    public ResponseEntity<Resource> getRegistrationFile(@PathVariable UUID id) {
        User current = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        FileMetadata metadata = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("File not found"));
        if (!current.getCompanyId().equals(metadata.getCompanyId())) {
            auditLogService.failure(current, "VIEW_REGISTRATION_FILE", null, null, "File belongs to another company: " + id);
            throw new org.springframework.security.access.AccessDeniedException("File belongs to another company");
        }
        Path path = Path.of(metadata.getStoragePath()).normalize();
        FileSystemResource resource = new FileSystemResource(path);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }
        auditLogService.success(current, "VIEW_REGISTRATION_FILE", null, null, null, id.toString());
        return ResponseEntity.ok()
                .contentType(mediaType(metadata, path))
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }

    @PostMapping("/admin/registration-requests/{id}/approve-with-department")
    public ApiResponse<RegistrationRequest> approveWithDepartment(@PathVariable UUID id, @RequestBody ApproveRegistrationRequest request) {
        return ApiResponse.success(registrationService.approve(id, request.getDepartment()), "Registration request approved");
    }

    @PostMapping("/admin/registration-requests/{id}/approve")
    public ApiResponse<RegistrationRequest> approve(@PathVariable UUID id, @RequestBody(required = false) ApproveRegistrationRequest request) {
        return ApiResponse.success(registrationService.approve(id), "Заявка одобрена. Создание AD/M365/ZKT ожидает подключения реальных сервисов.");
    }

    @PostMapping("/admin/registration-requests/{id}/reject")
    public ApiResponse<RegistrationRequest> reject(@PathVariable UUID id, @Valid @RequestBody RejectRegistrationRequest request) {
        return ApiResponse.success(registrationService.reject(id, request.getReason()), "Заявка отклонена");
    }

    @GetMapping("/registration/{token}")
    public ApiResponse<Map<String, Object>> validate(@PathVariable String token) {
        RegistrationLink link = registrationService.validate(token);
        return ApiResponse.success(Map.of("status", link.getStatus(), "expiresAt", link.getExpiresAt()));
    }

    @PostMapping("/registration/{token}/files")
    public ApiResponse<RegistrationFileUploadResponse> uploadFile(
            @PathVariable String token,
            @RequestParam String type,
            @RequestParam MultipartFile file
    ) {
        return ApiResponse.success(registrationService.uploadFile(token, type, file), "Файл загружен");
    }

    @PostMapping("/registration/{token}")
    public ApiResponse<RegistrationRequest> submit(@PathVariable String token, @Valid @RequestBody RegistrationSubmitRequest request) {
        return ApiResponse.success(registrationService.submit(token, request), "Заявка отправлена администратору");
    }
    private MediaType mediaType(FileMetadata metadata, Path path) {
        try {
            String type = metadata.getContentType() != null ? metadata.getContentType() : Files.probeContentType(path);
            return type != null ? MediaType.parseMediaType(type) : MediaType.APPLICATION_OCTET_STREAM;
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}
