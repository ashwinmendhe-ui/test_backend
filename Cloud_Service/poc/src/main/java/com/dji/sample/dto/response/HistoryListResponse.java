package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
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

    private UUID userId;
    private String userName;

    private UUID sessionId;

    private String startTime;
    private String endTime;
    private String totalTime;

    private String playbackUrl;

    private Integer totalRecognition;

    private List<String> detectionTypes;
    private String mainDetectionType;

    private String workIssue;

    private String createdAt;
    private String videoStatus;
}