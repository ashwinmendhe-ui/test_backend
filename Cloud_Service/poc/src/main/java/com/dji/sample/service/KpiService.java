package com.dji.sample.service;

import com.dji.sample.dto.kpi.response.KpiDeviceSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiMissionSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiSummaryResponse;

public interface KpiService {

    KpiSummaryResponse getSummary();

    KpiDeviceSummaryResponse getDeviceSummary();

    KpiMissionSummaryResponse getMissionSummary();
}