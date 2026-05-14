package com.orca.hrplatform.attendance.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZktecoPersonPhotoResponse {
    private String pin;
    private String name;
    private String lastName;
    private String photoPath;
}
