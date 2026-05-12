package com.orca.hrplatform.attendance.service;

import com.orca.hrplatform.attendance.dto.AttendanceHistoryResponse;
import com.orca.hrplatform.attendance.dto.AttendanceImportResponse;
import com.orca.hrplatform.attendance.dto.LateEmployeeResponse;
import com.orca.hrplatform.attendance.dto.LiveAttendanceResponse;
import com.orca.hrplatform.attendance.dto.TopLateResponse;
import com.orca.hrplatform.attendance.dto.ZktecoDepartmentResponse;
import com.orca.hrplatform.attendance.entity.AttendanceLog;
import com.orca.hrplatform.attendance.entity.AttendanceSource;
import com.orca.hrplatform.attendance.entity.AttendanceStatus;
import com.orca.hrplatform.attendance.repository.AttendanceLogRepository;
import com.orca.hrplatform.company.entity.Company;
import com.orca.hrplatform.company.repository.CompanyRepository;
import com.orca.hrplatform.employee.entity.Employee;
import com.orca.hrplatform.employee.repository.EmployeeRepository;
import com.orca.hrplatform.integration.zkteco.config.ZktecoProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ZktecoAttendanceService {
    private static final LocalTime WORK_START = LocalTime.of(9, 0);

    private final ObjectProvider<JdbcTemplate> zktecoJdbcTemplateProvider;
    private final ZktecoProperties zktecoProperties;
    private final AttendanceLogRepository attendanceLogRepository;
    private final EmployeeRepository employeeRepository;
    private final CompanyRepository companyRepository;

    public ZktecoAttendanceService(
            @Qualifier("zktecoJdbcTemplate") ObjectProvider<JdbcTemplate> zktecoJdbcTemplateProvider,
            ZktecoProperties zktecoProperties,
            AttendanceLogRepository attendanceLogRepository,
            EmployeeRepository employeeRepository,
            CompanyRepository companyRepository
    ) {
        this.zktecoJdbcTemplateProvider = zktecoJdbcTemplateProvider;
        this.zktecoProperties = zktecoProperties;
        this.attendanceLogRepository = attendanceLogRepository;
        this.employeeRepository = employeeRepository;
        this.companyRepository = companyRepository;
    }

    public List<LiveAttendanceResponse> liveEvents() {
        return latestStaffEvents(300).stream()
                .filter(this::isAttendanceEvent)
                .map(this::toLiveResponse)
                .toList();
    }

    public List<LiveAttendanceResponse> currentStaffStatus() {
        Map<String, ZktecoEvent> latestByPin = latestStaffEvents(2_000).stream()
                .filter(this::isAttendanceEvent)
                .collect(Collectors.toMap(
                        ZktecoEvent::pin,
                        event -> event,
                        (left, right) -> left.eventTime().isAfter(right.eventTime()) ? left : right,
                        LinkedHashMap::new
                ));

        return latestByPin.values().stream()
                .sorted(Comparator
                        .comparing((ZktecoEvent event) -> nullSafe(event.rootDepartment()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(event -> nullSafe(event.departmentName()), String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ZktecoEvent::eventTime, Comparator.reverseOrder()))
                .map(this::toLiveResponse)
                .toList();
    }

    public List<ZktecoDepartmentResponse> departments() {
        return jdbcTemplate().query("""
                WITH RECURSIVE dept_tree AS (
                    SELECT id, name, parent_id, name AS root_department
                    FROM auth_department
                    WHERE parent_id IS NULL
                    UNION ALL
                    SELECT d.id, d.name, d.parent_id, dt.root_department
                    FROM auth_department d
                    JOIN dept_tree dt ON d.parent_id = dt.id
                )
                SELECT d.name AS department_name,
                       dt.root_department,
                       COUNT(DISTINCT p.pin) AS employee_count
                FROM auth_department d
                LEFT JOIN dept_tree dt ON dt.id = d.id
                LEFT JOIN pers_person p ON p.auth_dept_id = d.id
                GROUP BY d.name, dt.root_department
                HAVING COUNT(DISTINCT p.pin) > 0
                ORDER BY dt.root_department ASC, d.name ASC
                """, (rs, rowNum) -> ZktecoDepartmentResponse.builder()
                        .name(rs.getString("department_name"))
                        .rootDepartment(rs.getString("root_department"))
                        .employeeCount(rs.getLong("employee_count"))
                        .build());
    }

    public List<LateEmployeeResponse> lateToday() {
        return eventsForPeriod(LocalDate.now().atStartOfDay(), LocalDate.now().plusDays(1).atStartOfDay(), 5_000).stream()
                .filter(this::isEntryEvent)
                .collect(Collectors.groupingBy(ZktecoEvent::pin))
                .values().stream()
                .map(events -> events.stream().min(Comparator.comparing(ZktecoEvent::eventTime)).orElseThrow())
                .filter(event -> event.eventTime().toLocalTime().isAfter(WORK_START))
                .sorted(Comparator.comparing(ZktecoEvent::eventTime).reversed())
                .map(event -> LateEmployeeResponse.builder()
                        .pin(event.pin())
                        .name(event.name())
                        .lastName(event.lastName())
                        .firstEntry(event.eventTime())
                        .delay(formatDelay(event.eventTime().toLocalTime()))
                        .build())
                .toList();
    }

    public List<TopLateResponse> topLateWeek() {
        LocalDateTime from = LocalDate.now().minusDays(7).atStartOfDay();
        return eventsForPeriod(from, LocalDate.now().plusDays(1).atStartOfDay(), 20_000).stream()
                .filter(this::isEntryEvent)
                .collect(Collectors.groupingBy(event -> event.pin() + "|" + event.eventTime().toLocalDate()))
                .values().stream()
                .map(events -> events.stream().min(Comparator.comparing(ZktecoEvent::eventTime)).orElseThrow())
                .filter(event -> event.eventTime().toLocalTime().isAfter(WORK_START))
                .collect(Collectors.groupingBy(ZktecoEvent::pin))
                .values().stream()
                .map(events -> {
                    ZktecoEvent sample = events.getFirst();
                    return TopLateResponse.builder()
                            .pin(sample.pin())
                            .name(sample.name())
                            .lastName(sample.lastName())
                            .lateCount(events.size())
                            .build();
                })
                .sorted(Comparator.comparingLong(TopLateResponse::getLateCount).reversed())
                .limit(20)
                .toList();
    }

    public List<AttendanceHistoryResponse> history(String pin, LocalDate date) {
        return jdbcTemplate().query("""
                SELECT t.unique_key,
                       t.pin,
                       t.name,
                       t.last_name,
                       t.event_point_name,
                       t.area_name,
                       t.event_time,
                       d.name AS department_name,
                       dt.root_department,
                       p.photo_path
                FROM acc_transaction t
                LEFT JOIN pers_person p ON p.pin = t.pin
                LEFT JOIN auth_department d ON d.id = p.auth_dept_id
                LEFT JOIN (
                    WITH RECURSIVE dept_tree AS (
                        SELECT id, name, parent_id, name AS root_department
                        FROM auth_department
                        WHERE parent_id IS NULL
                        UNION ALL
                        SELECT d2.id, d2.name, d2.parent_id, dt2.root_department
                        FROM auth_department d2
                        JOIN dept_tree dt2 ON d2.parent_id = dt2.id
                    )
                    SELECT * FROM dept_tree
                ) dt ON dt.id = p.auth_dept_id
                WHERE t.pin = ?
                  AND DATE(t.event_time) = ?
                ORDER BY t.event_time ASC
                """, eventMapper(), pin, date).stream()
                .filter(this::isAttendanceEvent)
                .map(event -> AttendanceHistoryResponse.builder()
                        .pin(event.pin())
                        .name(event.name())
                        .lastName(event.lastName())
                        .eventPointName(event.eventPointName())
                        .areaName(event.areaName())
                        .eventTime(event.eventTime())
                        .status(statusOf(event))
                        .build())
                .toList();
    }

    @Transactional
    public AttendanceImportResponse importRecentEvents() {
        List<ZktecoEvent> events = latestStaffEvents(zktecoProperties.getImportBatchSize()).stream()
                .filter(this::isAttendanceEvent)
                .sorted(Comparator.comparing(ZktecoEvent::eventTime))
                .toList();

        int imported = 0;
        int skipped = 0;
        Optional<Company> company = companyRepository.findAll().stream().findFirst();

        if (company.isEmpty()) {
            return AttendanceImportResponse.builder()
                    .scanned(events.size())
                    .imported(0)
                    .skipped(events.size())
                    .message("No HR company found")
                    .build();
        }

        for (ZktecoEvent event : events) {
            if (event.uniqueKey() == null || attendanceLogRepository.existsByExternalEventId(event.uniqueKey())) {
                skipped++;
                continue;
            }

            Optional<Employee> employee = employeeRepository.findFirstByZktecoPin(event.pin());
            if (employee.isEmpty()) {
                skipped++;
                continue;
            }

            upsertDailyLog(company.get(), employee.get(), event);
            imported++;
        }

        return AttendanceImportResponse.builder()
                .scanned(events.size())
                .imported(imported)
                .skipped(skipped)
                .message("ZKTeco import finished")
                .build();
    }

    private void upsertDailyLog(Company company, Employee employee, ZktecoEvent event) {
        LocalDate attendanceDate = event.eventTime().toLocalDate();
        AttendanceLog log = attendanceLogRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), attendanceDate)
                .stream()
                .findFirst()
                .orElseGet(() -> AttendanceLog.builder()
                        .companyId(company.getId())
                        .employeeId(employee.getId())
                        .attendanceDate(attendanceDate)
                        .status(AttendanceStatus.PRESENT)
                        .source(AttendanceSource.DEVICE)
                        .build());

        LocalTime eventTime = event.eventTime().toLocalTime();
        if (isEntryEvent(event)) {
            if (log.getCheckInTime() == null || eventTime.isBefore(log.getCheckInTime())) {
                log.setCheckInTime(eventTime);
            }
        } else if (isExitEvent(event)) {
            if (log.getCheckOutTime() == null || eventTime.isAfter(log.getCheckOutTime())) {
                log.setCheckOutTime(eventTime);
            }
        }

        if (log.getCheckInTime() != null && log.getCheckInTime().isAfter(WORK_START)) {
            log.setLate(true);
            log.setLateMinutes((int) Duration.between(WORK_START, log.getCheckInTime()).toMinutes());
            log.setStatus(AttendanceStatus.LATE);
        }

        log.setExternalDeviceId(event.areaName());
        log.setExternalEventId(event.uniqueKey());
        attendanceLogRepository.save(log);
    }

    private List<ZktecoEvent> latestStaffEvents(int limit) {
        return jdbcTemplate().query(baseStaffEventsSql("""
                ORDER BY t.event_time DESC
                LIMIT ?
                """), eventMapper(), limit);
    }

    private List<ZktecoEvent> eventsForPeriod(LocalDateTime from, LocalDateTime to, int limit) {
        return jdbcTemplate().query(baseStaffEventsSql("""
                AND t.event_time >= ?
                AND t.event_time < ?
                ORDER BY t.event_time DESC
                LIMIT ?
                """), eventMapper(), from, to, limit);
    }

    private String baseStaffEventsSql(String tail) {
        return """
                WITH RECURSIVE dept_tree AS (
                    SELECT id, name, parent_id, name AS root_department
                    FROM auth_department
                    WHERE parent_id IS NULL
                    UNION ALL
                    SELECT d.id, d.name, d.parent_id, dt.root_department
                    FROM auth_department d
                    JOIN dept_tree dt ON d.parent_id = dt.id
                )
                SELECT t.unique_key,
                       t.pin,
                       t.name,
                       t.last_name,
                       t.event_point_name,
                       t.area_name,
                       t.event_time,
                       d.name AS department_name,
                       dt.root_department,
                       p.photo_path
                FROM acc_transaction t
                LEFT JOIN pers_person p ON p.pin = t.pin
                LEFT JOIN auth_department d ON d.id = p.auth_dept_id
                LEFT JOIN dept_tree dt ON dt.id = p.auth_dept_id
                WHERE dt.root_department IS NOT NULL
                """ + tail;
    }

    private RowMapper<ZktecoEvent> eventMapper() {
        return (rs, rowNum) -> fromResultSet(rs);
    }

    private ZktecoEvent fromResultSet(ResultSet rs) throws SQLException {
        return new ZktecoEvent(
                rs.getString("unique_key"),
                rs.getString("pin"),
                rs.getString("name"),
                rs.getString("last_name"),
                rs.getString("event_point_name"),
                rs.getString("area_name"),
                rs.getTimestamp("event_time").toLocalDateTime(),
                rs.getString("department_name"),
                rs.getString("root_department"),
                rs.getString("photo_path")
        );
    }

    private LiveAttendanceResponse toLiveResponse(ZktecoEvent event) {
        return LiveAttendanceResponse.builder()
                .pin(event.pin())
                .name(event.name())
                .lastName(event.lastName())
                .eventPointName(event.eventPointName())
                .areaName(event.areaName())
                .eventTime(event.eventTime())
                .departmentName(event.departmentName())
                .rootDepartment(event.rootDepartment())
                .photoPath(event.photoPath())
                .status(statusOf(event))
                .build();
    }

    private boolean isAttendanceEvent(ZktecoEvent event) {
        return isEntryEvent(event) || isExitEvent(event);
    }

    private boolean isEntryEvent(ZktecoEvent event) {
        String text = normalize(event.eventPointName());
        return text.contains("entry") || text.contains("in") || text.contains("\u0432\u0445\u043e\u0434") || text.contains("ð’ñ…ð¾ð´");
    }

    private boolean isExitEvent(ZktecoEvent event) {
        String text = normalize(event.eventPointName());
        return text.contains("exit") || text.contains("out") || text.contains("\u0432\u044b\u0445\u043e\u0434") || text.contains("ð’ñ‹ñ…ð¾ð´");
    }

    private String statusOf(ZktecoEvent event) {
        if (isEntryEvent(event)) {
            return "INSIDE";
        }
        if (isExitEvent(event)) {
            return "OUTSIDE";
        }
        return "UNKNOWN";
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }

    private String formatDelay(LocalTime time) {
        long minutes = Duration.between(WORK_START, time).toMinutes();
        return minutes / 60 + "h " + minutes % 60 + "m";
    }

    private String nullSafe(String value) {
        return value == null ? "" : value;
    }

    private JdbcTemplate jdbcTemplate() {
        JdbcTemplate jdbcTemplate = zktecoJdbcTemplateProvider.getIfAvailable();
        if (jdbcTemplate == null) {
            throw new IllegalStateException("ZKTeco integration is disabled or not configured");
        }
        return jdbcTemplate;
    }

    private record ZktecoEvent(
            String uniqueKey,
            String pin,
            String name,
            String lastName,
            String eventPointName,
            String areaName,
            LocalDateTime eventTime,
            String departmentName,
            String rootDepartment,
            String photoPath
    ) {
    }
}
