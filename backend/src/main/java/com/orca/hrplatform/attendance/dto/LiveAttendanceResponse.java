package com.orca.hrplatform.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LiveAttendanceResponse {
    private String pin;
    private String name;
    private String lastName;
    private String eventPointName;
    private String areaName;
    private LocalDateTime eventTime;
    private String departmentName;
    private String rootDepartment;
    private String photoPath;
    private String status;
}
