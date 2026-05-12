package com.orca.hrplatform.request.repository;

import com.orca.hrplatform.request.entity.EmployeeRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmployeeRequestRepository extends JpaRepository<EmployeeRequest, UUID> {}
