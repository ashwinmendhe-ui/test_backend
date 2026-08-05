package com.dji.sample.controller;

import com.dji.sample.dto.kpi.request.KpiFilterRequest;
import com.dji.sample.dto.kpi.response.KpiDeviceSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiMissionSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiSummaryResponse;
import com.dji.sample.dto.response.ApiResponse;
import com.dji.sample.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/kpi")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    @GetMapping("/summary")
    public ApiResponse<KpiSummaryResponse> getSummary(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID missionId,
            @RequestParam(required = false) String deviceSn,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime toDate
    ) {

        KpiFilterRequest filter = buildFilter(
                companyId,
                siteId,
                missionId,
                deviceSn,
                fromDate,
                toDate
        );

        validateFilter(filter);

        return ApiResponse.<KpiSummaryResponse>builder()
                .success(true)
                .message("KPI summary fetched successfully")
                .data(kpiService.getSummary(filter))
                .build();
    }

    @GetMapping("/devices/summary")
    public ApiResponse<KpiDeviceSummaryResponse> getDeviceSummary(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID missionId,
            @RequestParam(required = false) String deviceSn,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime toDate
    ) {

        KpiFilterRequest filter = buildFilter(
                companyId,
                siteId,
                missionId,
                deviceSn,
                fromDate,
                toDate
        );

        validateFilter(filter);

        return ApiResponse.<KpiDeviceSummaryResponse>builder()
                .success(true)
                .message("KPI device summary fetched successfully")
                .data(kpiService.getDeviceSummary(filter))
                .build();
    }

    @GetMapping("/missions/summary")
    public ApiResponse<KpiMissionSummaryResponse> getMissionSummary(
            @RequestParam(required = false) UUID companyId,
            @RequestParam(required = false) UUID siteId,
            @RequestParam(required = false) UUID missionId,
            @RequestParam(required = false) String deviceSn,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime fromDate,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            OffsetDateTime toDate
    ) {

        KpiFilterRequest filter = buildFilter(
                companyId,
                siteId,
                missionId,
                deviceSn,
                fromDate,
                toDate
        );

        validateFilter(filter);

        return ApiResponse.<KpiMissionSummaryResponse>builder()
                .success(true)
                .message("KPI mission summary fetched successfully")
                .data(kpiService.getMissionSummary(filter))
                .build();
    }

    private KpiFilterRequest buildFilter(
            UUID companyId,
            UUID siteId,
            UUID missionId,
            String deviceSn,
            OffsetDateTime fromDate,
            OffsetDateTime toDate
    ) {
        KpiFilterRequest filter = new KpiFilterRequest();
        filter.setCompanyId(companyId);
        filter.setSiteId(siteId);
        filter.setMissionId(missionId);
        filter.setDeviceSn(deviceSn);
        filter.setFromDate(fromDate);
        filter.setToDate(toDate);
        return filter;
    }

    private void validateFilter(KpiFilterRequest filter) {
        if (filter == null) {
            return;
        }

        if (filter.getFromDate() != null
                && filter.getToDate() != null
                && filter.getFromDate().isAfter(filter.getToDate())) {
            throw new IllegalArgumentException("fromDate must be before toDate");
        }
    }
}