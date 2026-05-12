package com.orca.hrplatform.health;

import com.orca.hrplatform.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/health")
public class HealthController {

    @GetMapping
    public ApiResponse<Map<String, String>> health() {
        Map<String, String> healthInfo = new HashMap<>();
        healthInfo.put("status", "UP");
        healthInfo.put("service", "hr-peopleops-backend");
        healthInfo.put("timestamp", String.valueOf(System.currentTimeMillis()));
        
        return ApiResponse.success(healthInfo);
    }
}
