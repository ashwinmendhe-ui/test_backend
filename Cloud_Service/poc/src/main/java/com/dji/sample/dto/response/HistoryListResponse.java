package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class HistoryListResponse {
    private UUID historyId;

    private UUID companyId;
    private String companyName;

    private UUID siteId;
    private String siteName;

    private UUID missionId;
    private String missionName;

    private String deviceSn;
    private String deviceName;

    private String playbackUrl;

    private String userName;

    private Integer totalRecognition;
    private String createdAt;
    private String videoStatus;
}