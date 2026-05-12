package com.orca.hrplatform.position.repository;

import com.orca.hrplatform.position.entity.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PositionRepository extends JpaRepository<Position, UUID> {
    List<Position> findByCompanyId(UUID companyId);
}
