package com.orca.hrplatform.registration.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegistrationSubmitRequest {
    @NotBlank
    private String firstName;
    @NotBlank
    private String lastName;
    @NotBlank
    private String fullName;
    @NotBlank
    private String department;
    @NotBlank
    @Pattern(regexp = "^\\+?[0-9\\s()\\-]{7,25}$")
    private String phone;
    @Email
    @NotBlank
    private String personalEmail;
    @NotBlank
    private String identityDocumentFileId;
    @NotBlank
    private String facePhotoFileId;
}
