package com.orca.hrplatform.document.controller;

import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.document.dto.EmployeeFileUploadResponse;
import com.orca.hrplatform.document.service.EmployeeFileStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

@RestController
@RequestMapping("/employees/{employeeLogin}/files")
@RequiredArgsConstructor
public class EmployeeFileController {
    private final EmployeeFileStorageService employeeFileStorageService;

    @PostMapping
    public ApiResponse<EmployeeFileUploadResponse> upload(
            @PathVariable String employeeLogin,
            @RequestParam(defaultValue = "documents") String category,
            @RequestParam MultipartFile file
    ) throws IOException {
        return ApiResponse.success(employeeFileStorageService.save(employeeLogin, category, file), "File uploaded");
    }

    @GetMapping
    public ApiResponse<List<EmployeeFileUploadResponse>> list(
            @PathVariable String employeeLogin,
            @RequestParam(defaultValue = "documents") String category
    ) throws IOException {
        return ApiResponse.success(employeeFileStorageService.list(employeeLogin, category));
    }

    @GetMapping("/download")
    public ResponseEntity<Resource> download(
            @PathVariable String employeeLogin,
            @RequestParam(defaultValue = "documents") String category,
            @RequestParam String fileName
    ) throws IOException {
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
}
