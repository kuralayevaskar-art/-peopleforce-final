package com.orca.hrplatform.department.controller;

import com.orca.hrplatform.auth.entity.User;
import com.orca.hrplatform.auth.repository.UserRepository;
import com.orca.hrplatform.common.response.ApiResponse;
import com.orca.hrplatform.common.security.SecurityUtils;
import com.orca.hrplatform.department.entity.Department;
import com.orca.hrplatform.department.repository.DepartmentRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentRepository departmentRepository;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<List<Department>> list() {
        User current = userRepository.findByEmail(SecurityUtils.getCurrentUserEmail())
                .orElseThrow(() -> new EntityNotFoundException("Current user not found"));
        return ApiResponse.success(departmentRepository.findByCompanyId(current.getCompanyId()));
    }
}
