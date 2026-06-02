package com.dji.sample.service.impl;

import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.dto.response.HistoryListResponse;
import com.dji.sample.entity.*;
import com.dji.sample.repository.*;
import com.dji.sample.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final ReportHistoryRepository reportHistoryRepository;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<HistoryListResponse> getList() {
        return reportHistoryRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(ReportHistory::getCreatedAt).reversed())
                .map(this::toListResponse)
                .toList();
    }

    @Override
    public HistoryDetailResponse getDetail(UUID historyId) {
        ReportHistory history = reportHistoryRepository.findById(historyId)
                .orElseThrow(() -> new RuntimeException("History not found"));

        Site site = findSite(history.getSiteId());
        Mission mission = findMission(history.getMissionId());
        Device device = findDevice(history.getDeviceSn());
        User user = findUser(history.getUserId());

        String deviceName = device != null ? device.getDeviceName() : history.getDeviceSn();
        String userName = resolveUserName(user);

        return HistoryDetailResponse.builder()
                .deviceSn(history.getDeviceSn())
                .siteName(site != null ? site.getName() : "")
                .deviceName(deviceName)
                .companyId(history.getCompanyId())
                .siteId(history.getSiteId())
                .missionId(history.getMissionId())
                .robotName(deviceName)
                .missionName(mission != null ? mission.getMissionName() : "")
                .userName(userName)
                .workerName(userName)
                .startTime(format(history.getStartTime()))
                .endTime(format(history.getEndTime()))
                .totalTime(history.getTotalTime())
                .totalRecognition(history.getTotalRecognition())
                .duration(history.getTotalTime())
                .distance("")
                .playbackUrl(history.getPlaybackUrl())
                .reportCreatedAt(format(history.getEndTime() != null ? history.getEndTime() : history.getStartTime()))
                .labelCounts(Collections.emptyMap())
                .bookmarks(Collections.emptyList())
                .build();
    }

    private HistoryListResponse toListResponse(ReportHistory history) {
        Company company = findCompany(history.getCompanyId());
        Site site = findSite(history.getSiteId());
        Mission mission = findMission(history.getMissionId());
        Device device = findDevice(history.getDeviceSn());
        User user = findUser(history.getUserId());

        return HistoryListResponse.builder()
                .historyId(history.getHistoryId())
                .companyId(history.getCompanyId())
                .companyName(company != null ? company.getCompanyName() : "")
                .siteId(history.getSiteId())
                .siteName(site != null ? site.getName() : "")
                .missionId(history.getMissionId())
                .missionName(mission != null ? mission.getMissionName() : "")
                .deviceSn(history.getDeviceSn())
                .deviceName(device != null ? device.getDeviceName() : history.getDeviceSn())
                .playbackUrl(history.getPlaybackUrl())
                .userName(resolveUserName(user))
                .totalRecognition(history.getTotalRecognition())
                .createdAt(format(history.getCreatedAt()))
                .videoStatus(history.getVideoStatus())
                .build();
    }

    private Company findCompany(UUID companyId) {
        if (companyId == null) return null;
        return companyRepository.findById(companyId).orElse(null);
    }

    private Site findSite(UUID siteId) {
        if (siteId == null) return null;
        return siteRepository.findById(siteId).orElse(null);
    }

    private Mission findMission(UUID missionId) {
        if (missionId == null) return null;
        return missionRepository.findById(missionId).orElse(null);
    }

    private Device findDevice(String deviceSn) {
        if (deviceSn == null || deviceSn.isBlank()) return null;
        return deviceRepository.findByDeviceSn(deviceSn).orElse(null);
    }

    private User findUser(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).orElse(null);
    }

    private String resolveUserName(User user) {
        if (user == null) return "";
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        if (user.getUsername() != null && !user.getUsername().isBlank()) {
            return user.getUsername();
        }
        return user.getEmail() != null ? user.getEmail() : "";
    }

    private String format(OffsetDateTime value) {
        if (value == null) return "";
        return value.format(FORMATTER);
    }
}