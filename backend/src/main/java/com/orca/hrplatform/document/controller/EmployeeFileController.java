package com.orca.hrplatform.document.controller;

import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.common.security.SecurityUtils;
import com.orca.hrplatform.document.dto.EmployeeFileUploadResponse;
import com.orca.hrplatform.document.service.EmployeeFileStorageService;
import com.orca.hrplatform.employee.entity.Employee;
import com.orca.hrplatform.employee.repository.EmployeeRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/employees/{employeeLogin}/files")
@RequiredArgsConstructor
public class EmployeeFileController {
    private final EmployeeFileStorageService employeeFileStorageService;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    @PostMapping
    public ApiResponse<EmployeeFileUploadResponse> upload(
            @PathVariable String employeeLogin,
            @RequestParam(defaultValue = "documents") String category,
            @RequestParam MultipartFile file
    ) throws IOException {
        requireFileAccess(employeeLogin, category, true);
        return ApiResponse.success(employeeFileStorageService.save(employeeLogin, category, file), "File uploaded");
    }

    @GetMapping
    public ApiResponse<List<EmployeeFileUploadResponse>> list(
            @PathVariable String employeeLogin,
            @RequestParam(defaultValue = "documents") String category
    ) throws IOException {
        requireFileAccess(employeeLogin, category, false);
        return ApiResponse.success(employeeFileStorageService.list(employeeLogin, category));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @PathVariable String employeeLogin,
            @RequestParam(defaultValue = "documents") String category,
            @RequestParam String fileName
    ) throws IOException {
        requireFileAccess(employeeLogin, category, false);
        Path path = employeeFileStorageService.resolve(employeeLogin, category, fileName);

        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            return ResponseEntity.notFound().build();
        }

        String contentType = Files.probeContentType(path);
        Resource resource = new UrlResource(path.toUri());

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + path.getFileName() + "\"")
                .body(resource);
    }

    private void requireFileAccess(String employeeLogin, String category, boolean write) {
        User current = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        if (isAdmin(current)) {
            return;
        }
        if (!Set.of("photo", "other").contains(category)) {
            throw new AccessDeniedException("Only administrators can access employee documents");
        }
        if (current.getEmployeeId() == null) {
            throw new AccessDeniedException("Employee profile is not linked to current user");
        }
        Employee employee = employeeRepository.findById(current.getEmployeeId())
                .orElseThrow(() -> new EntityNotFoundException("Employee profile not found"));
        String currentLogin = safe(employee.getAdUsername());
        String currentEmail = safe(employee.getEmail());
        String requestedLogin = safe(employeeLogin);
        if (!requestedLogin.equals(currentLogin) && !requestedLogin.equals(currentEmail)) {
            throw new AccessDeniedException(write ? "You can upload files only to your own profile" : "You can view only your own files");
        }
    }

    private boolean isAdmin(User user) {
        return user.getRoles().stream()
                .anyMatch(role -> Set.of("SUPER_ADMIN", "HR_ADMIN", "INTEGRATION_ADMIN").contains(role.getCode()));
    }

    private String safe(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }
}
