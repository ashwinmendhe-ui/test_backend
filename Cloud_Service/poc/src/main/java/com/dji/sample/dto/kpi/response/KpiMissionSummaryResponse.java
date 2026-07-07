package com.dji.sample.dto.kpi.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiMissionSummaryResponse {

    private long totalMissions;
    private long completedMissions;
    private long failedMissions;
    private long interruptedMissions;

    private double successRate;

    private long totalOperationMinutes;
    private double averageMissionMinutes;

    private long totalAiDetections;
}