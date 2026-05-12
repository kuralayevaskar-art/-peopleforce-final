package com.orca.hrplatform.provisioning.service;

import com.orca.hrplatform.integration.synology.config.SynologyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SynologyPathService {
    private final SynologyProperties synologyProperties;

    public List<String> employeeFolders(String login) {
        String root = synologyProperties.getRootPath();
        String base = root.endsWith("\\") ? root + login : root + "\\" + login;
        return List.of(
                base,
                base + "\\agreement",
                base + "\\photo",
                base + "\\documents",
                base + "\\other"
        );
    }
}
