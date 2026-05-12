package com.orca.hrplatform.document.repository;

import com.orca.hrplatform.document.entity.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface DocumentTypeRepository extends JpaRepository<DocumentType, UUID> {}
