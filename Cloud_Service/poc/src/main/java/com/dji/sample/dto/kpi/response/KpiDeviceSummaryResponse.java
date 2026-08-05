package com.dji.sample.dto.kpi.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiDeviceSummaryResponse {

    private long totalDevices;
    private long robotCount;
    private long droneCount;

    private long onlineCount;
    private long workingCount;
    private long offlineCount;
}