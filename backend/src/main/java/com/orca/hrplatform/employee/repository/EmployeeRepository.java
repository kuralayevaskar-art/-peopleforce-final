package com.orca.hrplatform.employee.repository;

import com.orca.hrplatform.employee.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {
    List<Employee> findByCompanyId(UUID companyId);
    Optional<Employee> findFirstByZktecoPin(String zktecoPin);
    Optional<Employee> findFirstByAdUsernameIgnoreCaseOrEmailIgnoreCase(String adUsername, String email);
}
