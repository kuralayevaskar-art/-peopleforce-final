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
import java.time.LocalDate;
import java.util.List;

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
        if (zktecoProperties.getPhotoRoot() == null || zktecoProperties.getPhotoRoot().isBlank()) {
            return ResponseEntity.notFound().build();
        }

        String cleanPath = path.replace("\\", "/");
        while (cleanPath.startsWith("/")) {
            cleanPath = cleanPath.substring(1);
        }

        Path root = Path.of(zktecoProperties.getPhotoRoot()).normalize();
        Path fullPath = root.resolve(cleanPath).normalize();
        if (!fullPath.startsWith(root)) {
            return ResponseEntity.notFound().build();
        }

        FileSystemResource resource = new FileSystemResource(fullPath);
        if (!resource.exists() || !resource.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        MediaType mediaType = cleanPath.toLowerCase().endsWith(".png") ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .cacheControl(CacheControl.noCache())
                .body(resource);
    }
}
