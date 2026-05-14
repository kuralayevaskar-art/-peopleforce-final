package com.orca.hrplatform.integration.ad.service;

import com.orca.hrplatform.integration.ad.dto.AdPhotoSyncResponse;
import com.orca.hrplatform.integration.zkteco.service.ZktecoFacePhotoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdZktecoPhotoSyncService {
    private final AdDirectoryService adDirectoryService;
    private final ZktecoFacePhotoService zktecoFacePhotoService;

    public AdPhotoSyncResponse syncOne(String username) {
        try {
            AdDirectoryService.AdUserPhoto photo = adDirectoryService.loadUserPhoto(username);
            ZktecoFacePhotoService.ZktecoPhotoUploadResult result = zktecoFacePhotoService.uploadFacePhotoBytes(
                    photo.getUsername(),
                    photo.getContent(),
                    photo.getExtension()
            );
            return AdPhotoSyncResponse.builder()
                    .username(username)
                    .status(result.getStatus())
                    .message(result.getMessage())
                    .build();
        } catch (RuntimeException ex) {
            return AdPhotoSyncResponse.builder()
                    .username(username)
                    .status("ERROR")
                    .message(ex.getMessage())
                    .build();
        }
    }

    public List<AdPhotoSyncResponse> syncAll(List<AdDirectoryService.AdUser> users) {
        return users.stream()
                .map(AdDirectoryService.AdUser::getUsername)
                .map(this::syncOne)
                .toList();
    }
}
