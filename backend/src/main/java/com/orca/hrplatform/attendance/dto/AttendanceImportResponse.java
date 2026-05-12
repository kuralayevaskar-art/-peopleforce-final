package com.orca.hrplatform.attendance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AttendanceImportResponse {
    private int scanned;
    private int imported;
    private int skipped;
    private String message;
}
