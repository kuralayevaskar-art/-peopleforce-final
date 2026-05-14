package com.orca.hrplatform.employee.controller;

import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.employee.dto.EmployeeResponse;
import com.orca.hrplatform.employee.dto.EmployeeUpdateRequest;
import com.orca.hrplatform.employee.service.EmployeeAccessService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employees")
@RequiredArgsConstructor
public class EmployeeController {
    private final EmployeeAccessService employeeAccessService;

    @GetMapping
    public ApiResponse<List<EmployeeResponse>> list() {
        return ApiResponse.success(employeeAccessService.listVisible());
    }

    @GetMapping("/{id}")
    public ApiResponse<EmployeeResponse> get(@PathVariable UUID id) {
        return ApiResponse.success(employeeAccessService.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<EmployeeResponse> update(@PathVariable UUID id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return ApiResponse.success(employeeAccessService.update(id, request), "Данные сотрудника успешно обновлены.");
    }
}
