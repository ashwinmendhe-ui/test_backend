package com.dji.sample.service.impl;

import com.dji.sample.dto.response.HistoryDetailResponse;
import com.dji.sample.dto.response.HistoryListResponse;
import com.dji.sample.entity.*;
import com.dji.sample.repository.*;
import com.dji.sample.service.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.*;
import com.dji.sample.util.DateTimeUtil;
import com.dji.sample.dto.request.CreateHistoryRequest;
import java.time.Duration;



@Service
@RequiredArgsConstructor
public class HistoryServiceImpl implements HistoryService {

    private final ReportHistoryRepository reportHistoryRepository;
    private final CompanyRepository companyRepository;
    private final SiteRepository siteRepository;
    private final MissionRepository missionRepository;
    private final DeviceRepository deviceRepository;
    private final UserRepository userRepository;
    private final LiveStreamSessionRepository liveStreamSessionRepository;
    
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
                .companyName(company != null ? company.getName() : "")
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
        return companyRepository.findByCompanyIdAndDeletedAtIsNull(companyId).orElse(null);
    }

    private Site findSite(UUID siteId) {
        if (siteId == null) return null;
        return siteRepository.findBySiteIdAndDeletedAtIsNull(siteId).orElse(null);
    }

    private Mission findMission(UUID missionId) {
        if (missionId == null) return null;
        return missionRepository.findByMissionIdAndDeletedAtIsNull(missionId).orElse(null);
    }

    private Device findDevice(String deviceSn) {
        if (deviceSn == null || deviceSn.isBlank()) return null;
        return deviceRepository.findByDeviceSnAndDeletedAtIsNull(deviceSn).orElse(null);
    }

    private User findUser(UUID userId) {
        if (userId == null) return null;
        return userRepository.findByUserIdAndDeletedAtIsNull(userId).orElse(null);
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


    private String calculateTotalTime(OffsetDateTime startTime, OffsetDateTime endTime) {
        if (startTime == null || endTime == null) {
            return "00:00:00";
        }

        Duration duration = Duration.between(startTime, endTime);

        long seconds = Math.max(duration.getSeconds(), 0);
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;

        return String.format("%02d:%02d:%02d", hours, minutes, secs);
    }

    @Override
    public HistoryDetailResponse createHistory(CreateHistoryRequest request) {

        if (request.getDeviceSn() == null || request.getDeviceSn().isBlank()) {
            throw new RuntimeException("deviceSn is required");
        }

        if (request.getPlaybackUrl() == null || request.getPlaybackUrl().isBlank()) {
            throw new RuntimeException("playbackUrl is required");
        }

        Device device = findDevice(request.getDeviceSn());

        LiveStreamSession session = null;
        if (request.getMissionId() != null) {
            session = liveStreamSessionRepository
                    .findFirstByDeviceSnAndMissionIdOrderByStartedAtDesc(
                            request.getDeviceSn(),
                            request.getMissionId()
                    )
                    .orElse(null);
        }

        UUID companyId = null;
        UUID siteId = null;

        if (device != null) {
            companyId = device.getCompany() != null ? device.getCompany().getCompanyId() : null;
            siteId = device.getSite() != null ? device.getSite().getSiteId() : null;
        }

        OffsetDateTime startTime = session != null ? session.getStartedAt() : OffsetDateTime.now();
        OffsetDateTime endTime = session != null && session.getStoppedAt() != null
                ? session.getStoppedAt()
                : OffsetDateTime.now();

        ReportHistory history = ReportHistory.builder()
                .historyId(UUID.randomUUID())
                .deviceSn(request.getDeviceSn())
                .playbackUrl(request.getPlaybackUrl())
                .companyId(companyId)
                .siteId(siteId)
                .missionId(request.getMissionId())
                .userId(session != null ? session.getUserId() : null)
                .startTime(startTime)
                .endTime(endTime)
                .totalTime(calculateTotalTime(startTime, endTime))
                .totalRecognition(0)
                .createdAt(OffsetDateTime.now())
                .videoStatus("AVAILABLE")
                .build();

        ReportHistory saved = reportHistoryRepository.save(history);

        return getDetail(saved.getHistoryId());
    }
    private String format(OffsetDateTime value) {
        String formatted = DateTimeUtil.formatKst(value);
        return formatted != null ? formatted : "";
    }
}