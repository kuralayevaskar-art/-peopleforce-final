package com.orca.hrplatform.document.repository;

import com.orca.hrplatform.document.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface FileMetadataRepository extends JpaRepository<FileMetadata, UUID> {}
