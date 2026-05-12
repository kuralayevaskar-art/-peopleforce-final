package com.orca.hrplatform.company.repository;

import com.orca.hrplatform.company.entity.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {}
