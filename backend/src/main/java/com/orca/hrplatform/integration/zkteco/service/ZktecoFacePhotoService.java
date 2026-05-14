package com.orca.hrplatform.integration.zkteco.service;

import com.orca.hrplatform.document.entity.FileMetadata;
import com.orca.hrplatform.document.repository.FileMetadataRepository;
import com.orca.hrplatform.integration.zkteco.config.ZktecoProperties;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class ZktecoFacePhotoService {
    private final ObjectProvider<JdbcTemplate> zktecoJdbcTemplateProvider;
    private final ZktecoProperties properties;
    private final FileMetadataRepository fileMetadataRepository;

    public ZktecoFacePhotoService(
            @Qualifier("zktecoJdbcTemplate") ObjectProvider<JdbcTemplate> zktecoJdbcTemplateProvider,
            ZktecoProperties properties,
            FileMetadataRepository fileMetadataRepository
    ) {
        this.zktecoJdbcTemplateProvider = zktecoJdbcTemplateProvider;
        this.properties = properties;
        this.fileMetadataRepository = fileMetadataRepository;
    }

    public ZktecoPhotoUploadResult uploadFacePhoto(String pinOrLogin, String facePhotoRef) {
        return uploadFacePhoto(pinOrLogin, null, null, facePhotoRef);
    }

    public ZktecoPhotoUploadResult uploadFacePhoto(String pinOrLogin, String firstName, String lastName, String facePhotoRef) {
        JdbcTemplate jdbcTemplate = zktecoJdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null || !properties.isEnabled()) {
            return ZktecoPhotoUploadResult.skipped("ZKTeco integration is disabled or not configured");
        }
        if (!StringUtils.hasText(properties.getPhotoRoot())) {
            throw new IllegalStateException("ZKTeco photo root is not configured");
        }

        Path source = resolveFacePhoto(facePhotoRef);
        String extension = normalizePhotoExtension(extension(source));
        Optional<String> zktecoPin = findZktecoPin(jdbcTemplate, pinOrLogin, firstName, lastName);
        if (zktecoPin.isEmpty()) {
            return ZktecoPhotoUploadResult.skipped("ZKTeco user is not synced from AD yet: " + pinOrLogin);
        }
        String relativePath = zktecoRelativePhotoPath(zktecoPin.get(), extension);
        Path target = Path.of(properties.getPhotoRoot(), relativePath.replaceFirst("^/+", "")).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot copy Face ID photo to ZKTeco photo root", ex);
        }

        int updated = jdbcTemplate.update("UPDATE pers_person SET photo_path = ? WHERE lower(pin) = lower(?)",
                relativePath, zktecoPin.get());
        if (updated == 0) {
            return ZktecoPhotoUploadResult.skipped("ZKTeco user is not synced from AD yet: " + pinOrLogin);
        }
        upsertBiophoto(jdbcTemplate, zktecoPin.get(), source, extension);
        return ZktecoPhotoUploadResult.success(relativePath);
    }

    public ZktecoPhotoUploadResult uploadFacePhotoBytes(String pinOrLogin, byte[] content, String extension) {
        JdbcTemplate jdbcTemplate = zktecoJdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null || !properties.isEnabled()) {
            return ZktecoPhotoUploadResult.skipped("ZKTeco integration is disabled or not configured");
        }
        if (content == null || content.length == 0) {
            throw new IllegalStateException("Face ID photo content is empty");
        }
        if (!StringUtils.hasText(properties.getPhotoRoot())) {
            throw new IllegalStateException("ZKTeco photo root is not configured");
        }

        String safeExtension = normalizePhotoExtension(extension);
        String relativePath = zktecoRelativePhotoPath(pinOrLogin, safeExtension);
        Path target = Path.of(properties.getPhotoRoot(), relativePath.replaceFirst("^/+", "")).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot write AD photo to ZKTeco photo root", ex);
        }

        int updated = jdbcTemplate.update("UPDATE pers_person SET photo_path = ? WHERE lower(pin) = lower(?)",
                relativePath, pinOrLogin);
        if (updated == 0) {
            return ZktecoPhotoUploadResult.skipped("ZKTeco user is not synced from AD yet: " + pinOrLogin);
        }
        upsertBiophotoBytes(jdbcTemplate, pinOrLogin, content, safeExtension);
        return ZktecoPhotoUploadResult.success(relativePath);
    }

    private void upsertBiophoto(JdbcTemplate jdbcTemplate, String pin, Path source, String extension) {
        String relativePath = zktecoCropfacePath(pin, extension);
        Path target = Path.of(properties.getPhotoRoot(), relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot copy Face ID crop photo to ZKTeco photo root", ex);
        }
        upsertBiophotoRow(jdbcTemplate, pin, relativePath);
    }

    private void upsertBiophotoBytes(JdbcTemplate jdbcTemplate, String pin, byte[] content, String extension) {
        String relativePath = zktecoCropfacePath(pin, extension);
        Path target = Path.of(properties.getPhotoRoot(), relativePath).normalize();
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot write AD crop photo to ZKTeco photo root", ex);
        }
        upsertBiophotoRow(jdbcTemplate, pin, relativePath);
    }

    private void upsertBiophotoRow(JdbcTemplate jdbcTemplate, String pin, String relativePath) {
        List<String> personIds = jdbcTemplate.queryForList(
                "SELECT id FROM pers_person WHERE lower(pin) = lower(?) LIMIT 1",
                String.class,
                pin
        );
        if (personIds.isEmpty()) {
            return;
        }
        String personId = personIds.getFirst();
        int updated = jdbcTemplate.update("""
                UPDATE pers_biophoto
                SET photo_path = ?, update_time = now(), updater_code = 'admin', updater_name = 'admin'
                WHERE person_id = ? AND bio_type = 9
                """, relativePath, personId);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO pers_biophoto (
                      id, app_id, bio_tbl_id, company_id, create_time, creater_code, creater_id, creater_name,
                      op_version, update_time, updater_code, updater_id, updater_name, bio_type, person_id, person_pin, photo_path
                    ) VALUES (?, '', '', '', now(), 'admin', '', 'admin', 0, now(), 'admin', '', 'admin', 9, ?, ?, ?)
                    """, UUID.randomUUID().toString().replace("-", ""), personId, pin, relativePath);
        }
    }

    private Optional<String> findZktecoPin(JdbcTemplate jdbcTemplate, String pinOrLogin, String firstName, String lastName) {
        if (StringUtils.hasText(pinOrLogin)) {
            List<String> byPin = jdbcTemplate.queryForList(
                    "SELECT pin FROM pers_person WHERE lower(pin) = lower(?) LIMIT 1",
                    String.class,
                    pinOrLogin
            );
            if (!byPin.isEmpty()) {
                return Optional.ofNullable(byPin.getFirst());
            }
        }
        if (StringUtils.hasText(firstName) && StringUtils.hasText(lastName)) {
            List<String> byName = jdbcTemplate.queryForList("""
                    SELECT pin
                    FROM pers_person
                    WHERE lower(name) = lower(?)
                      AND lower(last_name) = lower(?)
                    ORDER BY pin DESC
                    LIMIT 1
                    """, String.class, firstName, lastName);
            if (!byName.isEmpty()) {
                return Optional.ofNullable(byName.getFirst());
            }
        }
        return Optional.empty();
    }

    private String zktecoRelativePhotoPath(String pinOrLogin, String extension) {
        String safeExtension = normalizePhotoExtension(extension);
        return "/upload/pers/user/avatar/" + LocalDate.now() + "/" + sanitize(pinOrLogin) + safeExtension;
    }

    private String zktecoCropfacePath(String pinOrLogin, String extension) {
        return "upload/pers/user/cropface/" + sanitize(pinOrLogin) + "/" + sanitize(pinOrLogin) + normalizePhotoExtension(extension);
    }

    private String normalizePhotoExtension(String extension) {
        String safeExtension = StringUtils.hasText(extension) ? extension.toLowerCase() : ".jpg";
        if (!safeExtension.startsWith(".")) {
            safeExtension = "." + safeExtension;
        }
        return ".jpeg".equals(safeExtension) ? ".jpg" : safeExtension;
    }

    private Path resolveFacePhoto(String ref) {
        Optional<Path> metadataPath = parseUuid(ref)
                .flatMap(fileMetadataRepository::findById)
                .map(FileMetadata::getStoragePath)
                .map(Path::of);
        Path path = metadataPath.orElseGet(() -> Path.of(ref));
        if (!Files.exists(path) || !Files.isRegularFile(path)) {
            throw new IllegalStateException("Face ID photo file not found: " + ref);
        }
        return path;
    }

    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String extension(Path path) {
        String name = path.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot).toLowerCase() : ".jpg";
    }

    private String sanitize(String value) {
        return value == null ? "unknown" : value.toLowerCase().replaceAll("[^a-z0-9._-]", "");
    }

    @Data
    @Builder
    public static class ZktecoPhotoUploadResult {
        private String status;
        private String message;

        static ZktecoPhotoUploadResult success(String path) {
            return ZktecoPhotoUploadResult.builder().status("SUCCESS").message(path).build();
        }

        static ZktecoPhotoUploadResult skipped(String message) {
            return ZktecoPhotoUploadResult.builder().status("SKIPPED").message(message).build();
        }
    }
}
