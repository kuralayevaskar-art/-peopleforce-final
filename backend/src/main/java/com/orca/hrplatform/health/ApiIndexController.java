package com.orca.hrplatform.health;

import com.orca.hrplatform.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ApiIndexController {

    @GetMapping("/")
    public ApiResponse<Map<String, Object>> index() {
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("service", "hr-peopleops-backend");
        info.put("status", "UP");
        info.put("frontend", "http://localhost:4200");
        info.put("health", "/api/v1/health");
        info.put("login", "POST /api/v1/auth/login");
        info.put("adLogin", "POST /api/v1/auth/ad/login");
        info.put("adSettings", "/api/v1/admin/integrations/ad/settings");

        return ApiResponse.success(info);
    }
}
