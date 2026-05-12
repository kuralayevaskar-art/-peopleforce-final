package com.orca.hrplatform.attendance.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class LateEmployeeResponse {
    private String pin;
    private String name;
    private String lastName;
    private LocalDateTime firstEntry;
    private String delay;
}
