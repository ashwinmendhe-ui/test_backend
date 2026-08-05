package com.dji.sample.service.impl;

import com.dji.sample.dto.request.CreateSiteRequest;
import com.dji.sample.dto.request.UpdateSiteRequest;
import com.dji.sample.dto.response.SiteResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Mission;
import com.dji.sample.entity.Site;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.MissionRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.service.SiteService;
import com.dji.sample.util.DateTimeUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.dji.sample.entity.User;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;
    private final CompanyRepository companyRepository;
    private final MissionRepository missionRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    public List<SiteResponse> searchSites(UUID companyId) {
        User currentUser = getCurrentUser();
        List<Site> sites;

        if (isSysAdmin(currentUser)) {
            if (companyId != null) {
                sites = siteRepository.findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(companyId);
            } else {
                sites = siteRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
            }
        } else {
            UUID effectiveCompanyId = currentUser.getCompanyId();

            if (effectiveCompanyId == null) {
                sites = List.of();
            } else {
                sites = siteRepository.findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(effectiveCompanyId);
            }
        }

        return sites.stream()
                .map(this::toResponse)
                .toList();
    }
    @Override
    public SiteResponse getSiteById(UUID id) {
        Site site = siteRepository.findBySiteIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        return toResponse(site);
    }

    @Override
    @Transactional
    public SiteResponse createSite(CreateSiteRequest request) {

        UUID companyId = request.getCompanyId();

        if (companyId != null) {
            companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));
        }

        Site site = Site.builder()
                .companyId(companyId)
                .name(request.getName())
                .address(request.getAddress())
                .description(request.getDescription())
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .timezone("Asia/Seoul")
                .build();

        Site saved = siteRepository.save(site);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public SiteResponse updateSite(UUID id, UpdateSiteRequest request) {

        Site site = siteRepository.findBySiteIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        UUID companyId = request.getCompanyId();

        if (companyId != null) {
            companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));
        }

        site.setCompanyId(companyId);
        site.setName(request.getName());
        site.setAddress(request.getAddress());
        site.setDescription(request.getDescription());
        site.setPhoneNumber(request.getPhoneNumber());
        site.setEmail(request.getEmail());

        Site saved = siteRepository.save(site);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void deleteSite(UUID id) {

        Site site = siteRepository.findBySiteIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        site.setDeletedAt(OffsetDateTime.now());
        siteRepository.save(site);

        List<Mission> missions =
                missionRepository.findBySiteIdAndDeletedAtIsNull(id);

        for (Mission mission : missions) {
            mission.setSiteId(null);
            missionRepository.save(mission);
        }
    }

    private String formatKst(OffsetDateTime dateTime) {
        return DateTimeUtil.formatKst(dateTime);
    }

    private String resolveCompanyName(UUID companyId) {

        if (companyId == null) {
            return null;
        }

        return companyRepository
                .findByCompanyIdAndDeletedAtIsNull(companyId)
                .map(Company::getName)
                .orElse(null);
    }

    private User getCurrentUser() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

    if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails customUserDetails)) {
        throw new RuntimeException("Authenticated user not found");
    }

    return userRepository.findByUserIdAndDeletedAtIsNull(customUserDetails.getUserId())
            .orElseThrow(() -> new RuntimeException("User not found"));
}

private boolean isSysAdmin(User user) {
    return userRoleRepository.existsByUserIdAndRoleId(user.getUserId(), 1);
}

    private SiteResponse toResponse(Site site) {

        boolean active = site.getDeletedAt() == null;

        var devices = deviceRepository
                .findBySite_SiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(site.getSiteId());

        int registeredDrone = devices.size();

        int onlineDrone = (int) devices.stream()
                .filter(device -> "WORKING".equalsIgnoreCase(device.getStatus())
                        || "ONLINE".equalsIgnoreCase(device.getStatus()))
                .count();

        return SiteResponse.builder()
                .siteId(site.getSiteId())
                .companyId(site.getCompanyId())
                .companyName(resolveCompanyName(site.getCompanyId()))
                .name(site.getName())
                .siteName(site.getName())
                .address(site.getAddress())
                .description(site.getDescription())
                .isActive(active)
                .createdAt(formatKst(site.getCreatedAt()))
                .updatedAt(formatKst(site.getUpdatedAt()))
                .phoneNumber(site.getPhoneNumber())
                .email(site.getEmail())
                .deviceCount(registeredDrone)
                .deviceOnlineCount(onlineDrone)
                
                .build();
    }
}