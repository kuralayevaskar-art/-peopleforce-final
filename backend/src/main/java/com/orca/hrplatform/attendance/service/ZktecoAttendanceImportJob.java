package com.orca.hrplatform.attendance.service;

import com.orca.hrplatform.integration.zkteco.config.ZktecoProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ZktecoAttendanceImportJob {
    private final ZktecoProperties zktecoProperties;
    private final ZktecoAttendanceService zktecoAttendanceService;

    @Scheduled(fixedDelayString = "${app.zkteco.import-fixed-delay-ms:300000}")
    public void importRecentEvents() {
        if (!zktecoProperties.isEnabled()) {
            return;
        }

        try {
            zktecoAttendanceService.importRecentEvents();
        } catch (RuntimeException error) {
            log.warn("ZKTeco attendance import failed: {}", error.getMessage());
        }
    }
}
