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
import com.dji.sample.entity.User;
import com.dji.sample.repository.UserRepository;
import com.dji.sample.repository.UserRoleRepository;
import com.dji.sample.security.CustomUserDetails;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@Service
@RequiredArgsConstructor
@Transactional
public class MissionServiceImpl implements MissionService {

    private final MissionRepository missionRepository;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;
    private final S3PresignService s3PresignService;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

   @Override
    @Transactional(readOnly = true)
    public List<MissionResponse> search(String keyword, String from, String to) {
        User currentUser = getCurrentUser();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();

        List<Mission> missions;

        boolean isCompanyUser = userRoleRepository.existsByUserIdAndRoleId(currentUser.getUserId(), 3);

        if (isSysAdmin(currentUser)) {
            missions = missionRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        } else if (isCompanyUser) {
            missions = currentUser.getMissions() == null
                    ? List.of()
                    : currentUser.getMissions().stream()
                            .filter(mission -> mission.getDeletedAt() == null)
                            .sorted(Comparator.comparing(Mission::getCreatedAt).reversed())
                            .toList();
        } else {
            UUID companyId = currentUser.getCompanyId();
            missions = companyId == null
                    ? List.of()
                    : missionRepository.findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(companyId);
        }

        return missions.stream()
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
    User currentUser = getCurrentUser();
    List<Mission> missions;

    if (isSysAdmin(currentUser)) {
        if (siteId != null && !siteId.isBlank()) {
            missions = missionRepository
                    .findBySiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID.fromString(siteId));
        } else if (companyId != null && !companyId.isBlank()) {
            missions = missionRepository
                    .findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID.fromString(companyId));
        } else {
            missions = missionRepository.findByDeletedAtIsNullOrderByCreatedAtDesc();
        }
    } else {
        boolean isCompanyUser =
                userRoleRepository.existsByUserIdAndRoleId(currentUser.getUserId(), 3);

        if (isCompanyUser) {
            UUID requestedSiteId =
                    siteId != null && !siteId.isBlank() ? UUID.fromString(siteId) : null;

            missions = currentUser.getMissions() == null
                    ? List.of()
                    : currentUser.getMissions().stream()
                            .filter(mission -> mission.getDeletedAt() == null)
                            .filter(mission -> requestedSiteId == null
                                    || requestedSiteId.equals(mission.getSiteId()))
                            .sorted(Comparator.comparing(Mission::getCreatedAt).reversed())
                            .toList();
        } else {
            UUID effectiveCompanyId = currentUser.getCompanyId();

            if (effectiveCompanyId == null) {
                return List.of();
            }

            if (siteId != null && !siteId.isBlank()) {
                UUID requestedSiteId = UUID.fromString(siteId);

                missions = missionRepository
                        .findBySiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(requestedSiteId)
                        .stream()
                        .filter(mission -> effectiveCompanyId.equals(mission.getCompanyId()))
                        .toList();
            } else {
                missions = missionRepository
                        .findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(effectiveCompanyId);
            }
        }
    }

    return missions.stream()
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