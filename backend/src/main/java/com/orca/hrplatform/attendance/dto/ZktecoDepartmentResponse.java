package com.orca.hrplatform.attendance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZktecoDepartmentResponse {
    private String name;
    private String rootDepartment;
    private long employeeCount;
}
