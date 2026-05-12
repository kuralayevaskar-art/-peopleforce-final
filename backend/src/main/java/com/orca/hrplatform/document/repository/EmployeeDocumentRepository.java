package com.orca.hrplatform.document.repository;

import com.orca.hrplatform.document.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {}
