package com.orca.hrplatform.registration.repository;

import com.orca.hrplatform.registration.entity.RegistrationRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface RegistrationRequestRepository extends JpaRepository<RegistrationRequest, UUID> {
    List<RegistrationRequest> findByCompanyIdOrderByCreatedAtDesc(UUID companyId);
    List<RegistrationRequest> findTop50ByStatusOrderByCreatedAtAsc(String status);
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
}
