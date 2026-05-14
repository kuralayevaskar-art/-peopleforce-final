package com.orca.hrplatform.registration.repository;

import com.orca.hrplatform.registration.entity.RegistrationLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RegistrationLinkRepository extends JpaRepository<RegistrationLink, UUID> {
    Optional<RegistrationLink> findByTokenHash(String tokenHash);
}
