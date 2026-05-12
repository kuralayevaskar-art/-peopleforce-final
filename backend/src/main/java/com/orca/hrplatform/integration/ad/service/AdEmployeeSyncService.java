package com.orca.hrplatform.integration.ad.service;

import com.orca.hrplatform.company.entity.Company;
import com.orca.hrplatform.company.entity.CompanyStatus;
import com.orca.hrplatform.company.repository.CompanyRepository;
import com.orca.hrplatform.employee.entity.Employee;
import com.orca.hrplatform.employee.entity.EmployeeStatus;
import com.orca.hrplatform.employee.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdEmployeeSyncService {
    private final CompanyRepository companyRepository;
    private final EmployeeRepository employeeRepository;

    @Transactional
    public List<Employee> sync(List<AdDirectoryService.AdUser> adUsers) {
        Company company = companyRepository.findAll().stream()
                .findFirst()
                .orElseGet(this::createDefaultCompany);

        return adUsers.stream()
                .filter(user -> StringUtils.hasText(user.getUsername()) || StringUtils.hasText(user.getEmail()))
                .map(user -> upsert(company, user))
                .toList();
    }

    private Company createDefaultCompany() {
        return companyRepository.save(Company.builder()
                .name("DMUK")
                .bin("DMUK")
                .email("hr@dmuk.edu.kz")
                .status(CompanyStatus.ACTIVE)
                .build());
    }

    private Employee upsert(Company company, AdDirectoryService.AdUser adUser) {
        String username = trimLower(adUser.getUsername());
        String email = trimLower(StringUtils.hasText(adUser.getEmail())
                ? adUser.getEmail()
                : username + "@dmuk.edu.kz");

        Employee employee = employeeRepository
                .findFirstByAdUsernameIgnoreCaseOrEmailIgnoreCase(username, email)
                .orElseGet(Employee::new);

        NameParts nameParts = splitName(adUser.getDisplayName(), username);
        employee.setCompanyId(company.getId());
        employee.setAdUsername(username);
        employee.setEmail(email);
        employee.setFirstName(nameParts.firstName());
        employee.setLastName(nameParts.lastName());
        employee.setMiddleName(nameParts.middleName());
        employee.setFullName(nameParts.fullName());
        employee.setStatus(EmployeeStatus.ACTIVE);

        if (!StringUtils.hasText(employee.getZktecoPin()) && StringUtils.hasText(username)) {
            employee.setZktecoPin(username);
        }

        return employeeRepository.save(employee);
    }

    private NameParts splitName(String displayName, String fallback) {
        String fullName = StringUtils.hasText(displayName) ? displayName.trim() : fallback;
        String[] parts = fullName.split("\\s+");
        String firstName = parts.length > 1 ? parts[1] : parts[0];
        String lastName = parts.length > 0 ? parts[0] : fallback;
        String middleName = parts.length > 2 ? parts[2] : null;
        return new NameParts(firstName, lastName, middleName, fullName);
    }

    private String trimLower(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private record NameParts(String firstName, String lastName, String middleName, String fullName) {
    }
}
