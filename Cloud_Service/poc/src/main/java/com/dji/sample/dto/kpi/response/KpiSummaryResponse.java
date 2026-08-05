package com.dji.sample.dto.kpi.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiSummaryResponse {

    private KpiDeviceSummaryResponse deviceSummary;
    private KpiMissionSummaryResponse missionSummary;
}