package com.orca.hrplatform.document.service;

import com.orca.hrplatform.document.dto.EmployeeFileUploadResponse;
import com.orca.hrplatform.integration.synology.config.SynologyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class EmployeeFileStorageService {
    private static final Set<String> ALLOWED_CATEGORIES = Set.of("agreement", "photo", "documents", "other");

    private final SynologyProperties synologyProperties;

    public EmployeeFileUploadResponse save(String employeeLogin, String category, MultipartFile file) throws IOException {
        String safeCategory = ALLOWED_CATEGORIES.contains(category) ? category : "documents";
        String safeLogin = sanitize(employeeLogin);
        String safeFileName = sanitizeFileName(file.getOriginalFilename());
        Path folder = Path.of(synologyProperties.getRootPath(), safeLogin, safeCategory);
        Path target = folder.resolve(safeFileName).normalize();

        if (!target.startsWith(folder.normalize())) {
            throw new IllegalArgumentException("Invalid file path");
        }

        Files.createDirectories(folder);
        file.transferTo(target);

        return EmployeeFileUploadResponse.builder()
                .fileName(safeFileName)
                .category(safeCategory)
                .storagePath(target.toString())
                .contentType(Files.probeContentType(target))
                .size(file.getSize())
                .build();
    }

    public List<EmployeeFileUploadResponse> list(String employeeLogin, String category) throws IOException {
        String safeCategory = ALLOWED_CATEGORIES.contains(category) ? category : "documents";
        String safeLogin = sanitize(employeeLogin);
        Path folder = Path.of(synologyProperties.getRootPath(), safeLogin, safeCategory);

        if (!Files.exists(folder)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(folder)) {
            return files
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> path.getFileName().toString().toLowerCase()))
                    .map(path -> toResponse(path, safeCategory))
                    .toList();
        }
    }

    public Path resolve(String employeeLogin, String category, String fileName) {
        String safeCategory = ALLOWED_CATEGORIES.contains(category) ? category : "documents";
        String safeLogin = sanitize(employeeLogin);
        String safeFileName = sanitizeFileName(fileName);
        Path folder = Path.of(synologyProperties.getRootPath(), safeLogin, safeCategory).normalize();
        Path target = folder.resolve(safeFileName).normalize();

        if (!target.startsWith(folder)) {
            throw new IllegalArgumentException("Invalid file path");
        }

        return target;
    }

    private EmployeeFileUploadResponse toResponse(Path path, String category) {
        try {
            return EmployeeFileUploadResponse.builder()
                    .fileName(path.getFileName().toString())
                    .category(category)
                    .storagePath(path.toString())
                    .contentType(Files.probeContentType(path))
                    .size(Files.size(path))
                    .build();
        } catch (IOException error) {
            throw new IllegalStateException("Cannot read file metadata", error);
        }
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value.toLowerCase().replaceAll("[^a-z0-9._-]", "");
    }

    private String sanitizeFileName(String fileName) {
        String fallback = "document";
        String value = fileName == null || fileName.isBlank() ? fallback : fileName;
        return value.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
