package com.dji.sample.service;

import com.dji.sample.dto.kpi.response.KpiDeviceSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiMissionSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiSummaryResponse;
import com.dji.sample.dto.kpi.request.KpiFilterRequest;

public interface KpiService {

    KpiSummaryResponse getSummary(KpiFilterRequest filter);

    KpiDeviceSummaryResponse getDeviceSummary(KpiFilterRequest filter);

    KpiMissionSummaryResponse getMissionSummary(KpiFilterRequest filter);
}