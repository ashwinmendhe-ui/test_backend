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
import com.dji.sample.service.S3PresignService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
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
    private final S3PresignService s3PresignService;

    @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> search(String keyword, String from, String to) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();

        return missionRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .filter(mission -> normalizedKeyword.isEmpty()
                        || contains(mission.getMissionName(), normalizedKeyword)
                        || contains(resolveCompanyName(mission.getCompanyId()), normalizedKeyword)
                        || contains(resolveSiteName(mission.getSiteId()), normalizedKeyword)
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
                    .findBySiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID.fromString(siteId))
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        if (companyId != null && !companyId.isBlank()) {
            return missionRepository
                    .findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID.fromString(companyId))
                    .stream()
                    .map(this::toResponse)
                    .toList();
        }

        return missionRepository.findByDeletedAtIsNullOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public MissionResponse getById(UUID id) {
        Mission mission = missionRepository.findByMissionIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Mission not found"));

        return toResponse(mission);
    }

    @Override
    public MissionResponse create(MissionRequest request) {
        String deviceType = normalizeDeviceType(request.getDeviceType());

        Mission mission = Mission.builder()
                .companyId(request.getCompanyId())
                .siteId(request.getSiteId())
                .missionName(request.getMissionName())
                .missionType(request.getMissionType())
                .deviceType(deviceType)
                .file(request.getFile())
                .description(request.getDescription())
                .build();

        validateCompanyAndSite(mission);

        Mission saved = missionRepository.save(mission);

        if (request.getFile() != null && !request.getFile().isBlank()) {
            String objectKey = buildObjectKey(saved);

            String uploadUrl = s3PresignService.createUploadUrl(objectKey);

            MissionResponse response = toResponse(saved);
            response.setId(saved.getMissionId().toString());
            response.setCode(0);
            response.setUploadUrl(uploadUrl);
            response.setObjectKey(objectKey);

            return response;
        }

        return toResponse(saved);
    }
    
    @Override
    public MissionResponse update(UUID id, MissionRequest request) {
        Mission mission = missionRepository.findByMissionIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Mission not found"));

        String deviceType = normalizeDeviceType(request.getDeviceType());

        mission.setCompanyId(request.getCompanyId());
        mission.setSiteId(request.getSiteId());
        mission.setMissionName(request.getMissionName());
        mission.setMissionType(request.getMissionType());
        mission.setDeviceType(deviceType);
        mission.setFile(request.getFile());
        mission.setDescription(request.getDescription());

        validateCompanyAndSite(mission);

        Mission saved = missionRepository.save(mission);

        if (request.getFile() != null && !request.getFile().isBlank()) {
            String objectKey = buildObjectKey(saved);

            String uploadUrl = s3PresignService.createUploadUrl(objectKey);

            MissionResponse response = toResponse(saved);
            response.setId(saved.getMissionId().toString());
            response.setCode(0);
            response.setUploadUrl(uploadUrl);
            response.setObjectKey(objectKey);

            return response;
        }

        return toResponse(saved);
    }
    @Override
    public void delete(UUID id) {
        Mission mission = missionRepository.findByMissionIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new EntityNotFoundException("Mission not found"));

        mission.setDeletedAt(OffsetDateTime.now());
        missionRepository.save(mission);
    }

    private void validateCompanyAndSite(Mission mission) {
        if (mission.getCompanyId() != null) {
            companyRepository.findByCompanyIdAndDeletedAtIsNull(mission.getCompanyId())
                    .orElseThrow(() -> new EntityNotFoundException("Company not found"));
        }

        if (mission.getSiteId() != null) {
            Site site = siteRepository.findBySiteIdAndDeletedAtIsNull(mission.getSiteId())
                    .orElseThrow(() -> new EntityNotFoundException("Site not found"));

            if (mission.getCompanyId() == null && site.getCompanyId() != null) {
                mission.setCompanyId(site.getCompanyId());
            }
        }
    }

    private String resolveCompanyName(UUID companyId) {
        if (companyId == null) {
            return null;
        }

        return companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId)
                .map(Company::getName)
                .orElse(null);
    }

    private String resolveSiteName(UUID siteId) {
        if (siteId == null) {
            return null;
        }

        return siteRepository.findBySiteIdAndDeletedAtIsNull(siteId)
                .map(Site::getName)
                .orElse(null);
    }

    private String buildObjectKey(Mission mission) {
        if (mission.getMissionId() == null || mission.getFile() == null || mission.getFile().isBlank()) {
            return null;
        }

        return "missions/" + mission.getMissionId() + "/" + mission.getFile();
    }

    private String resolveDownloadUrl(Mission mission) {
        String objectKey = buildObjectKey(mission);

        if (objectKey == null) {
            return null;
        }

        try {
            return s3PresignService.createDownloadUrl(
                    objectKey,
                    mission.getFile()
            );
        } catch (Exception e) {
            return null;
        }
    }
    private String normalizeDeviceType(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String trimmed = value.trim();

        if ("DRONE".equalsIgnoreCase(trimmed)) {
            return "Drone";
        }

        if ("ROBOT".equalsIgnoreCase(trimmed)
                || "Quadruped Robot".equalsIgnoreCase(trimmed)) {
            return "Robot";
        }

        return trimmed;
    }
    private MissionResponse toResponse(Mission mission) {
        String objectKey = buildObjectKey(mission);

        return MissionResponse.builder()
                .missionId(mission.getMissionId())
                .companyId(mission.getCompanyId())
                .companyName(resolveCompanyName(mission.getCompanyId()))
                .siteId(mission.getSiteId())
                .siteName(resolveSiteName(mission.getSiteId()))
                .missionName(mission.getMissionName())
                .name(mission.getMissionName())
                .missionType(mission.getMissionType())
                .deviceType(mission.getDeviceType())
                .file(mission.getFile())
                .fileName(mission.getFile())
                .downloadUrl(resolveDownloadUrl(mission))
                .description(mission.getDescription())
                .createdAt(mission.getCreatedAt())
                .id(mission.getMissionId().toString())
                .code(0)
                .objectKey(objectKey)
                .build();
    }

    private boolean contains(String value, String keyword) {
        return value != null && value.toLowerCase().contains(keyword);
    }
}