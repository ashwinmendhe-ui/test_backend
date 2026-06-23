package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
public class HistoryDetailResponse {
    private String deviceSn;
    private String siteName;
    private String deviceName;

    private UUID companyId;
    private UUID siteId;
    private UUID missionId;

    private String robotName;
    private String missionName;

    private String userName;
    private String workerName;

    private String startTime;
    private String endTime;
    private String totalTime;
    private Integer totalRecognition;

    private String duration;
    private String distance;

    private String playbackUrl;
    private String reportCreatedAt;

    private Map<String, Integer> labelCounts;
    private List<BookmarkResponse> bookmarks;
    private String companyName;
}