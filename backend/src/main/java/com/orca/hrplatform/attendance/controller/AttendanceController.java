package com.orca.hrplatform.attendance.controller;

import com.orca.hrplatform.attendance.dto.AttendanceHistoryResponse;
import com.orca.hrplatform.attendance.dto.AttendanceImportResponse;
import com.orca.hrplatform.attendance.dto.LateEmployeeResponse;
import com.orca.hrplatform.attendance.dto.LiveAttendanceResponse;
import com.orca.hrplatform.attendance.dto.TopLateResponse;
import com.orca.hrplatform.attendance.dto.ZktecoDepartmentResponse;
import com.orca.hrplatform.attendance.service.ZktecoAttendanceService;
import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.integration.zkteco.config.ZktecoProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Path;
import java.net.URI;
import java.net.HttpURLConnection;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.Duration;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

@RestController
@RequestMapping("/attendance")
@RequiredArgsConstructor
public class AttendanceController {
    private final ZktecoAttendanceService zktecoAttendanceService;
    private final ZktecoProperties zktecoProperties;

    @GetMapping("/live")
    public ApiResponse<List<LiveAttendanceResponse>> liveEvents() {
        return ApiResponse.success(zktecoAttendanceService.liveEvents());
    }

    @GetMapping("/inside")
    public ApiResponse<List<LiveAttendanceResponse>> currentStaffStatus() {
        return ApiResponse.success(zktecoAttendanceService.currentStaffStatus());
    }

    @GetMapping("/departments")
    public ApiResponse<List<ZktecoDepartmentResponse>> departments() {
        return ApiResponse.success(zktecoAttendanceService.departments());
    }

    @GetMapping("/late/today")
    public ApiResponse<List<LateEmployeeResponse>> lateToday() {
        return ApiResponse.success(zktecoAttendanceService.lateToday());
    }

    @GetMapping("/late/week/top")
    public ApiResponse<List<TopLateResponse>> topLateWeek() {
        return ApiResponse.success(zktecoAttendanceService.topLateWeek());
    }

    @GetMapping("/history")
    public ApiResponse<List<AttendanceHistoryResponse>> history(
            @RequestParam String pin,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        return ApiResponse.success(zktecoAttendanceService.history(pin, date));
    }

    @PostMapping("/import")
    public ApiResponse<AttendanceImportResponse> importRecentEvents() {
        return ApiResponse.success(zktecoAttendanceService.importRecentEvents());
    }

    @GetMapping("/photo")
    public ResponseEntity<Resource> getPhoto(@RequestParam String path) {
        String cleanPath = path.replace("\\", "/");
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        if (zktecoProperties.getPhotoRoot() != null && !zktecoProperties.getPhotoRoot().isBlank()) {
            Path root = Path.of(zktecoProperties.getPhotoRoot()).normalize();
            Path fullPath = root.resolve(cleanPath).normalize();
            if (!fullPath.startsWith(root)) {
                return ResponseEntity.notFound().build();
            }

            FileSystemResource resource = new FileSystemResource(fullPath);
            if (resource.exists() && resource.isReadable()) {
                return ResponseEntity.ok()
                        .contentType(mediaTypeFor(cleanPath, null))
                        .cacheControl(CacheControl.noCache())
                        .body(resource);
            }
        }

        return fetchRemoteZktecoPhoto(cleanPath);
    }

    private ResponseEntity<Resource> fetchRemoteZktecoPhoto(String cleanPath) {
        List<String> candidates = List.of(
                "https://" + zktecoProperties.getHost() + ":8088/" + cleanPath,
                "https://" + zktecoProperties.getHost() + ":8098/" + cleanPath
        );

        for (String url : candidates) {
            try {
                HttpURLConnection connection = (HttpURLConnection) URI.create(url).toURL().openConnection();
                connection.setConnectTimeout((int) Duration.ofSeconds(6).toMillis());
                connection.setReadTimeout((int) Duration.ofSeconds(6).toMillis());
                if (connection instanceof HttpsURLConnection httpsConnection) {
                    httpsConnection.setSSLSocketFactory(insecureSocketFactory());
                    httpsConnection.setHostnameVerifier((hostname, session) -> true);
                }

                if (connection.getResponseCode() >= 200 && connection.getResponseCode() < 300) {
                    byte[] bytes = connection.getInputStream().readAllBytes();
                    if (!looksLikeImage(bytes, cleanPath)) {
                        continue;
                    }
                    ByteArrayResource resource = new ByteArrayResource(bytes);
                    String contentType = connection.getContentType();
                    return ResponseEntity.ok()
                            .contentType(mediaTypeFor(cleanPath, contentType))
                            .cacheControl(CacheControl.noCache())
                            .body(resource);
                }
            } catch (Exception ignored) {
                // Try next known ZKTeco web port.
            }
        }

        return ResponseEntity.notFound().build();
    }

    private boolean looksLikeImage(byte[] bytes, String path) {
        if (bytes.length < 4) {
            return false;
        }
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return (bytes[0] & 0xFF) == 0xFF && (bytes[1] & 0xFF) == 0xD8 && (bytes[2] & 0xFF) == 0xFF;
        }
        if (lowerPath.endsWith(".png")) {
            return (bytes[0] & 0xFF) == 0x89 && bytes[1] == 0x50 && bytes[2] == 0x4E && bytes[3] == 0x47;
        }
        return true;
    }

    private MediaType mediaTypeFor(String path, String contentType) {
        String lowerPath = path.toLowerCase();
        if (lowerPath.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lowerPath.endsWith(".jpg") || lowerPath.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (contentType != null && contentType.toLowerCase().startsWith("image/")) {
            return MediaType.parseMediaType(contentType.split(";")[0]);
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private SSLSocketFactory insecureSocketFactory() throws Exception {
        TrustManager[] trustManagers = new TrustManager[] {
                new X509TrustManager() {
                    public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                    public void checkServerTrusted(X509Certificate[] chain, String authType) {}
                    public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
                }
        };
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, trustManagers, new SecureRandom());
        return sslContext.getSocketFactory();
    }
}
