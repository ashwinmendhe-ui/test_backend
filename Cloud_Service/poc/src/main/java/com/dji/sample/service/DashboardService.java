package com.dji.sample.service;

import com.dji.sample.dto.response.DashboardStatsResponse;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final DeviceRepository deviceRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;

    public DashboardStatsResponse getStats() {
        return DashboardStatsResponse.builder()
                .totalCompanies(companyRepository.count())
                .totalDevices(deviceRepository.count())
                .totalSites(siteRepository.count())
                .totalUsers(userRepository.count())
                .build();
    }
}