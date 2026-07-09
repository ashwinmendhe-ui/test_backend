package com.dji.sample.service;

import com.dji.sample.dto.response.DashboardStatsResponse;
import com.dji.sample.entity.User;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.security.CustomUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;


import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final CompanyRepository companyRepository;
    private final DeviceRepository deviceRepository;
    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    public DashboardStatsResponse getStats() {
    User currentUser = getCurrentUser();
    UUID userId = currentUser.getUserId();

    boolean isSysAdmin = userRoleRepository.existsByUserIdAndRoleId(userId, 1);
    boolean isCompanyUser = userRoleRepository.existsByUserIdAndRoleId(userId, 3);

    if (isSysAdmin) {
        return DashboardStatsResponse.builder()
                .totalCompanies(companyRepository.countByDeletedAtIsNull())
                .totalDevices(deviceRepository.countByDeletedAtIsNull())
                .totalSites(siteRepository.countByDeletedAtIsNull())
                .totalUsers(userRepository.countByDeletedAtIsNull())
                .build();
    }

    if (isCompanyUser) {
        long assignedDeviceCount = currentUser.getDevices() == null
                ? 0L
                : currentUser.getDevices().stream()
                        .filter(device -> device.getDeletedAt() == null)
                        .count();

        long assignedSiteCount = currentUser.getSites() == null
                ? 0L
                : currentUser.getSites().stream()
                        .filter(site -> site.getDeletedAt() == null)
                        .count();

        return DashboardStatsResponse.builder()
                .totalCompanies(1L)
                .totalDevices(assignedDeviceCount)
                .totalSites(assignedSiteCount)
                .totalUsers(1L)
                .build();
    }

    UUID companyId = currentUser.getCompanyId();

    if (companyId == null) {
        return DashboardStatsResponse.builder()
                .totalCompanies(0L)
                .totalDevices(0L)
                .totalSites(0L)
                .totalUsers(0L)
                .build();
    }

    return DashboardStatsResponse.builder()
            .totalCompanies(1L)
            .totalDevices(deviceRepository.countByCompany_CompanyIdAndDeletedAtIsNull(companyId))
            .totalSites(siteRepository.countByCompanyIdAndDeletedAtIsNull(companyId))
            .totalUsers(userRepository.countByCompanyIdAndDeletedAtIsNull(companyId))
            .build();
}
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
            throw new RuntimeException("Authenticated user not found");
        }

        return userRepository.findByUserIdAndDeletedAtIsNull(customUserDetails.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }
}