package com.dji.sample.controller;

import com.dji.sample.dto.response.DashboardStatsResponse;
import com.dji.sample.dto.response.ApiResponse;
import com.dji.sample.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

   @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStats() {

        return ApiResponse.<DashboardStatsResponse>builder()
                .success(true)
                .message("Dashboard stats fetched successfully")
                .data(dashboardService.getStats())
                .build();
    }
}