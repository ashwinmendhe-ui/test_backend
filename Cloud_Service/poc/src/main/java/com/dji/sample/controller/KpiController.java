package com.dji.sample.controller;

import com.dji.sample.dto.kpi.response.KpiSummaryResponse;
import com.dji.sample.dto.response.ApiResponse;
import com.dji.sample.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.dji.sample.dto.kpi.response.KpiDeviceSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiMissionSummaryResponse;

@RestController
@RequestMapping("/api/v1/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    @GetMapping("/summary")
    public ApiResponse<KpiSummaryResponse> getSummary() {
        return ApiResponse.<KpiSummaryResponse>builder()
                .success(true)
                .message("KPI summary fetched successfully")
                .data(kpiService.getSummary())
                .build();
    }

    @GetMapping("/devices/summary")
    public ApiResponse<KpiDeviceSummaryResponse> getDeviceSummary() {
        return ApiResponse.<KpiDeviceSummaryResponse>builder()
                .success(true)
                .message("KPI device summary fetched successfully")
                .data(kpiService.getDeviceSummary())
                .build();
    }

    @GetMapping("/missions/summary")
    public ApiResponse<KpiMissionSummaryResponse> getMissionSummary() {
        return ApiResponse.<KpiMissionSummaryResponse>builder()
                .success(true)
                .message("KPI mission summary fetched successfully")
                .data(kpiService.getMissionSummary())
                .build();
    }

}