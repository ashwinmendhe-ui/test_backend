package com.dji.sample.service.impl;

import com.dji.sample.dto.request.MissionRequest;
import com.dji.sample.dto.response.MissionResponse;
import com.dji.sample.entity.Company;
import com.dji.sample.entity.Mission;
import com.dji.sample.entity.Site;
import com.dji.sample.repository.CompanyRepository;
import com.dji.sample.repository.MissionRepository;
import com.dji.sample.repository.SiteRepository;
import com.dji.sample.service.MissionService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionServiceImpl implements MissionService {

    private final MissionRepository missionRepository;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> search(String keyword, String from, String to) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();

        return missionRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .filter(mission -> normalizedKeyword.isEmpty()
                        || contains(mission.getMissionName(), normalizedKeyword)
                        || contains(mission.getCompanyName(), normalizedKeyword)
                        || contains(mission.getSiteName(), normalizedKeyword)
                        || contains(mission.getMissionType(), normalizedKeyword)
                        || contains(mission.getDeviceType(), normalizedKeyword)
                        || contains(mission.getFile(), normalizedKeyword))
                .sorted(Comparator.comparing(Mission::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> list(String companyId, String siteId) {
        if (siteId != null && !siteId.isBlank()) {
            return missionRepository
                    .findBySiteIdAndIsActiveTrueOrderByCreatedAtDesc(UUID.fromString(siteId))
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        if (companyId != null && !companyId.isBlank()) {
            return missionRepository
                    .findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(UUID.fromString(companyId))
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return missionRepository.findByIsActiveTrueOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MissionResponse getById(UUID id) {
        Mission mission = missionRepository.findById(id)
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .orElseThrow(() -> new EntityNotFoundException("Mission not found"));

        return toResponse(mission);
    }

    @Override
    public MissionResponse create(MissionRequest request) {
        Mission mission = Mission.builder()
                .companyId(request.getCompanyId())
                .siteId(request.getSiteId())
                .missionName(request.getMissionName())
                .missionType(request.getMissionType())
                .deviceType(request.getDeviceType())
                .file(request.getFile())
                .downloadUrl(request.getDownloadUrl())
                .description(request.getDescription())
                .isActive(true)
                .build();

        enrichCompanyAndSite(mission, request);

        Mission saved = missionRepository.save(mission);
        return toResponse(saved);
    }

    @Override
    public MissionResponse update(UUID id, MissionRequest request) {
        Mission mission = missionRepository.findById(id)
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .orElseThrow(() -> new EntityNotFoundException("Mission not found"));

        mission.setCompanyId(request.getCompanyId());
        mission.setSiteId(request.getSiteId());
        mission.setMissionName(request.getMissionName());
        mission.setMissionType(request.getMissionType());
        mission.setDeviceType(request.getDeviceType());
        mission.setFile(request.getFile());
        mission.setDownloadUrl(request.getDownloadUrl());
        mission.setDescription(request.getDescription());

        enrichCompanyAndSite(mission, request);

        Mission saved = missionRepository.save(mission);
        return toResponse(saved);
    }

    @Override
    public void delete(UUID id) {
        Mission mission = missionRepository.findById(id)
                .filter(m -> Boolean.TRUE.equals(m.getIsActive()))
                .orElseThrow(() -> new EntityNotFoundException("Mission not found"));

        mission.setIsActive(false);
        missionRepository.save(mission);
    }

    private void enrichCompanyAndSite(Mission mission, MissionRequest request) {
        if (request.getCompanyId() != null) {
            companyRepository.findById(request.getCompanyId())
                    .map(Company::getCompanyName)
                    .ifPresent(mission::setCompanyName);
        } else {
            mission.setCompanyName(null);
        }

        if (request.getSiteId() != null) {
            siteRepository.findById(request.getSiteId())
                    .ifPresent(site -> {
                        mission.setSiteName(site.getName());

                        if (mission.getCompanyId() == null && site.getCompanyId() != null) {
                            mission.setCompanyId(site.getCompanyId());
                        }

                        if (mission.getCompanyName() == null || mission.getCompanyName().isBlank()) {
                            mission.setCompanyName(site.getCompanyName());
                        }
                    });
        } else {
            mission.setSiteName(null);
        }
    }

    private MissionResponse toResponse(Mission mission) {
        return MissionResponse.builder()
                .missionId(mission.getMissionId())
                .companyId(mission.getCompanyId())
                .companyName(mission.getCompanyName())
                .siteId(mission.getSiteId())
                .siteName(mission.getSiteName())
                .missionName(mission.getMissionName())
                .name(mission.getMissionName())
                .missionType(mission.getMissionType())
                .deviceType(mission.getDeviceType())
                .file(mission.getFile())
                .fileName(mission.getFile())
                .downloadUrl(mission.getDownloadUrl())
                .description(mission.getDescription())
                .createdAt(mission.getCreatedAt())
                .build();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}