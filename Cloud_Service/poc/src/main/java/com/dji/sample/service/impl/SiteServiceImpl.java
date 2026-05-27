package com.dji.sample.service.impl;

import com.dji.sample.dto.request.CreateSiteRequest;
import com.dji.sample.dto.request.UpdateSiteRequest;
import com.dji.sample.dto.response.SiteResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Site;
import com.dji.sample.entity.Mission;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.MissionRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.service.SiteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;



@Service
@RequiredArgsConstructor
public class SiteServiceImpl implements SiteService {

    private final SiteRepository siteRepository;
    private final CompanyRepository companyRepository;
    private final MissionRepository missionRepository;

    @Override
    public List<SiteResponse> searchSites(UUID companyId) {
        List<Site> sites;

        if (companyId != null) {
            sites = siteRepository.findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(companyId);
        } else {
            sites = siteRepository.findByIsActiveTrueOrderByCreatedAtDesc();
        }

        return sites.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public SiteResponse getSiteById(UUID id) {
        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        return toResponse(site);
    }

    @Override
    @Transactional
    public SiteResponse createSite(CreateSiteRequest request) {
        String companyName = null;
        UUID companyId = request.getCompanyId();

        if (companyId != null) {
            Company company = companyRepository.findById(companyId)
                    .orElseThrow(() -> new RuntimeException("Company not found"));
            companyName = company.getCompanyName();
        }

        Site site = Site.builder()
                .companyId(companyId)
                .companyName(companyName)
                .name(request.getName())
                .address(request.getAddress())
                .description(request.getDescription())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .phoneNumber(request.getPhoneNumber())
                .email(request.getEmail())
                .build();

        Site saved = siteRepository.save(site);
        return toResponse(saved);
    }
    @Override
    @Transactional
    public SiteResponse updateSite(UUID id, UpdateSiteRequest request) {
    Site site = siteRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Site not found"));

    String companyName = null;
    UUID companyId = request.getCompanyId();

    if (companyId != null) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));
        companyName = company.getCompanyName();
    }

    site.setCompanyId(companyId);
    site.setCompanyName(companyName);
    site.setName(request.getName());
    site.setAddress(request.getAddress());
    site.setDescription(request.getDescription());
    site.setPhoneNumber(request.getPhoneNumber());
    site.setEmail(request.getEmail());

    if (request.getIsActive() != null) {
        site.setIsActive(request.getIsActive());
    }

    Site saved = siteRepository.save(site);
    return toResponse(saved);
}
    @Override
    @Transactional
    public void deleteSite(UUID id) {

        Site site = siteRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Site not found"));

        // soft delete site
        site.setIsActive(false);
        siteRepository.save(site);

        // cleanup missions linked to this site
        List<Mission> missions =
                missionRepository.findBySiteIdAndIsActiveTrue(id);

        for (Mission mission : missions) {
            mission.setSiteId(null);
            mission.setSiteName(null);

            missionRepository.save(mission);
        }
    }
        


    private String formatKst(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }

        return dateTime
                .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private SiteResponse toResponse(Site site) {
        return SiteResponse.builder()
                .siteId(site.getSiteId())
                .companyId(site.getCompanyId())
                .companyName(site.getCompanyName())
                .name(site.getName())
                .siteName(site.getName())
                .address(site.getAddress())
                .description(site.getDescription())
                .isActive(site.getIsActive())
                .createdAt(formatKst(site.getCreatedAt()))
                .updatedAt(formatKst(site.getUpdatedAt()))
                .phoneNumber(site.getPhoneNumber())
                .email(site.getEmail())
                .build();
    }
}