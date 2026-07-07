package com.dji.sample.service.impl;

import com.dji.sample.dto.kpi.request.KpiFilterRequest;
import com.dji.sample.dto.kpi.response.KpiDeviceSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiMissionSummaryResponse;
import com.dji.sample.dto.kpi.response.KpiSummaryResponse;
import com.dji.sample.entity.Device;
import com.dji.sample.entity.ReportHistory;
import com.dji.sample.entity.SubDevice;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.repository.ReportHistoryRepository;
import com.dji.sample.repository.SubDeviceRepository;
import com.dji.sample.service.IDeviceRedisService;
import com.dji.sample.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class KpiServiceImpl implements KpiService {

    private final DeviceRepository deviceRepository;
    private final ReportHistoryRepository reportHistoryRepository;
    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final IDeviceRedisService deviceRedisService;
    private final SubDeviceRepository subDeviceRepository;

    @Override
    public KpiSummaryResponse getSummary(KpiFilterRequest filter) {
        return KpiSummaryResponse.builder()
                .deviceSummary(getDeviceSummary(filter))
                .missionSummary(getMissionSummary(filter))
                .build();
    }

    @Override
    public KpiDeviceSummaryResponse getDeviceSummary(KpiFilterRequest filter) {
        List<Device> devices = deviceRepository.findByDeletedAtIsNull();

        long totalDevices = devices.size();
        long robotCount = devices.stream()
                .filter(device -> "ROBOT".equals(normalizeDeviceType(device.getDeviceType())))
                .count();

        long droneCount = devices.stream()
                .filter(device -> "DRONE".equals(normalizeDeviceType(device.getDeviceType())))
                .count();
        long onlineCount = 0;
        long workingCount = 0;
        long offlineCount = 0;

        for (Device device : devices) {
            String deviceSn = device.getDeviceSn();

            if (deviceSn == null || deviceSn.isBlank()) {
                offlineCount++;
                continue;
            }

            boolean online = deviceRedisService.getDeviceOnline(deviceSn) != null;
            boolean working = online && liveStreamSessionRepository
                    .existsByDeviceSnAndSessionStatus(resolveStreamDeviceSn(deviceSn), "ACTIVE");

            if (working) {
                workingCount++;
            } else if (online) {
                onlineCount++;
            } else {
                offlineCount++;
            }
        }

        return KpiDeviceSummaryResponse.builder()
                .totalDevices(totalDevices)
                .robotCount(robotCount)
                .droneCount(droneCount)
                .onlineCount(onlineCount)
                .workingCount(workingCount)
                .offlineCount(offlineCount)
                .build();
    }
    @Override
    public KpiMissionSummaryResponse getMissionSummary(KpiFilterRequest filter) {
        List<ReportHistory> histories = reportHistoryRepository.findAll();

        long totalMissions = histories.size();

        long completedMissions = histories.stream()
                .filter(history -> "AVAILABLE".equalsIgnoreCase(history.getVideoStatus()))
                .count();

        long failedMissions = histories.stream()
            .filter(history -> "UNAVAILABLE".equalsIgnoreCase(history.getVideoStatus()))
            .count();
        long interruptedMissions = 0;

        long totalOperationMinutes = histories.stream()
                .mapToLong(this::calculateOperationMinutes)
                .sum();

        double averageMissionMinutes = totalMissions == 0
                ? 0
                : (double) totalOperationMinutes / totalMissions;

        long totalAiDetections = histories.stream()
                .map(ReportHistory::getTotalRecognition)
                .filter(value -> value != null)
                .mapToLong(Integer::longValue)
                .sum();

        double successRate = totalMissions == 0
                ? 0
                : ((double) completedMissions / totalMissions) * 100;

        return KpiMissionSummaryResponse.builder()
                .totalMissions(totalMissions)
                .completedMissions(completedMissions)
                .failedMissions(failedMissions)
                .interruptedMissions(interruptedMissions)
                .successRate(round(successRate))
                .totalOperationMinutes(totalOperationMinutes)
                .averageMissionMinutes(round(averageMissionMinutes))
                .totalAiDetections(totalAiDetections)
                .build();
    }

    private long calculateOperationMinutes(ReportHistory history) {
        if (history.getStartTime() == null || history.getEndTime() == null) {
            return 0;
        }

        long minutes = Duration.between(history.getStartTime(), history.getEndTime()).toMinutes();
        return Math.max(minutes, 0);
    }

    private String resolveStreamDeviceSn(String deviceSn) {
        if (deviceSn == null || deviceSn.isBlank()) {
            return deviceSn;
        }

        return subDeviceRepository
                .findFirstByDeviceSnAndDeletedAtIsNullOrderByIdAsc(deviceSn)
                .map(SubDevice::getSn)
                .filter(sn -> sn != null && !sn.isBlank())
                .orElse(deviceSn);
    }


    private String normalizeDeviceType(String deviceType) {
    if (deviceType == null || deviceType.isBlank()) {
        return "UNKNOWN";
    }

    String value = deviceType.trim();

    if ("Robot".equalsIgnoreCase(value)
            || "ROBOT".equalsIgnoreCase(value)
            || "Quadruped Robot".equalsIgnoreCase(value)
            || "4족보행 로봇".equalsIgnoreCase(value)) {
        return "ROBOT";
    }

    if ("Drone".equalsIgnoreCase(value)
            || "DRONE".equalsIgnoreCase(value)
            || "드론".equalsIgnoreCase(value)) {
        return "DRONE";
    }

    return "UNKNOWN";
}

    private double round(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}