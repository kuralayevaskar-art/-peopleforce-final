package com.orca.hrplatform.attendance.repository;

import com.orca.hrplatform.attendance.entity.AttendanceLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AttendanceLogRepository extends JpaRepository<AttendanceLog, UUID> {
    List<AttendanceLog> findByCompanyIdAndAttendanceDate(UUID companyId, LocalDate date);
    List<AttendanceLog> findByEmployeeIdAndAttendanceDate(UUID employeeId, LocalDate date);
    boolean existsByExternalEventId(String externalEventId);
}
