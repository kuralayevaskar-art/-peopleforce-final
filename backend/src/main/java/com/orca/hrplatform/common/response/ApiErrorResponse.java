package com.orca.hrplatform.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiErrorResponse {
    private boolean success;
    private String error;
    private String message;
    private List<String> details;
    private LocalDateTime timestamp;

    public static ApiErrorResponse of(String error, String message, List<String> details) {
        return ApiErrorResponse.builder()
                .success(false)
                .error(error)
                .message(message)
                .details(details)
                .timestamp(LocalDateTime.now())
                .build();
    }
}
