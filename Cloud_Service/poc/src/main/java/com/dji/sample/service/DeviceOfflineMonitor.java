package com.dji.sample.service;

import com.dji.sample.entity.Device;
import com.dji.sample.entity.LiveStreamSession;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.repository.LiveStreamSessionRepository;
import com.dji.sample.repository.SubDeviceRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceOfflineMonitor {

    private final DeviceRepository deviceRepository;
    private final StringRedisTemplate redisTemplate;
    private final DeviceWebSocketPublisher webSocketPublisher;
    private final LiveStreamSessionRepository liveStreamSessionRepository;
    private final SubDeviceRepository subDeviceRepository;
    private final IDeviceRedisService deviceRedisService;

    private final Set<String> lastOnlineDevices =
            ConcurrentHashMap.newKeySet();

    @Scheduled(fixedDelay = 10000)
    public void checkOfflineDevices() {

        deviceRepository.findByDeletedAtIsNull()
                .forEach(this::checkDevice);
    }

    private void checkDevice(Device device) {

        String deviceSn = device.getDeviceSn();

        if (deviceSn == null || deviceSn.isBlank()) {
            return;
        }

        String onlineKey = "online:" + deviceSn;

        Boolean online =
                redisTemplate.hasKey(onlineKey);

        /*
         * Device is currently online.
         *
         * Remember it so that we can detect the
         * ONLINE -> OFFLINE transition later.
         */
        if (Boolean.TRUE.equals(online)) {

            lastOnlineDevices.add(deviceSn);

            return;
        }

        /*
         * The online Redis key is now missing.
         *
         * Cleanup only when this backend instance
         * previously observed the device online.
         *
         * This prevents repeatedly cleaning every
         * offline device every 10 seconds.
         */
        boolean wasOnlineBefore =
                lastOnlineDevices.remove(deviceSn);

        if (!wasOnlineBefore) {
            return;
        }

        log.warn(
                "[DeviceOfflineMonitor] Device offline detected. deviceSn={}",
                deviceSn
        );

        try {

            cleanupOfflineRuntimeState(device);

        } catch (Exception exception) {

            log.error(
                    "[DeviceOfflineMonitor] Offline cleanup failed. deviceSn={}, error={}",
                    deviceSn,
                    exception.getMessage(),
                    exception
            );
        }

        /*
         * Publish OFFLINE state regardless of whether
         * one cleanup operation failed.
         */
        try {

            webSocketPublisher.publishDashboardStatus(
                    deviceSn,
                    "OFFLINE",
                    "device-offline-monitor"
            );

            webSocketPublisher.publishDashboardRefresh(
                    deviceSn,
                    "device-offline-monitor"
            );

        } catch (Exception exception) {

            log.warn(
                    "[DeviceOfflineMonitor] Dashboard notification failed. deviceSn={}, error={}",
                    deviceSn,
                    exception.getMessage(),
                    exception
            );
        }
    }

    private void cleanupOfflineRuntimeState(Device device) {

        String deviceSn = device.getDeviceSn();

        /*
         * Robot:
         *   deviceSn == livestream session deviceSn
         *
         * DJI:
         *   dashboard device can be gateway SN while
         *   livestream session belongs to camera/sub-device SN.
         */
        String streamDeviceSn =
                resolveStreamDeviceSn(deviceSn);

        /*
         * 1. Stop stale ACTIVE livestream session.
         */
        LiveStreamSession activeSession =
                liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(
                                streamDeviceSn,
                                "ACTIVE"
                        )
                        .orElse(null);

        if (activeSession != null) {

            activeSession.setSessionStatus("STOPPED");

            activeSession.setStoppedAt(
                    OffsetDateTime.now(ZoneOffset.UTC)
            );

            liveStreamSessionRepository.save(activeSession);

            log.warn(
                    "[DeviceOfflineMonitor] Stale ACTIVE session marked STOPPED. deviceSn={}, streamDeviceSn={}, sessionId={}",
                    deviceSn,
                    streamDeviceSn,
                    activeSession.getId()
            );
        }

        /*
         * 2. Clear stale mission stored directly
         *    against Device.
         */
        if (device.getMissionId() != null) {

            device.setMissionId(null);

            deviceRepository.save(device);

            log.info(
                    "[DeviceOfflineMonitor] Device mission cleared. deviceSn={}",
                    deviceSn
            );
        }

        /*
         * 3. Clear robot runtime state.
         *
         * Do NOT delete online:{deviceSn}.
         * That key already expired naturally and represents
         * physical device health.
         */
        deviceRedisService.clearRobotJobState(deviceSn);

        deviceRedisService.clearDeviceStatus(deviceSn);

        log.info(
                "[DeviceOfflineMonitor] Runtime state cleared. deviceSn={}",
                deviceSn
        );
    }

    private String resolveStreamDeviceSn(String deviceSn) {

        if (deviceSn == null || deviceSn.isBlank()) {
            return deviceSn;
        }

        return subDeviceRepository
                .findFirstByDeviceSnAndDeletedAtIsNullOrderByIdAsc(deviceSn)
                .map(subDevice -> subDevice.getSn())
                .filter(sn -> sn != null && !sn.isBlank())
                .orElse(deviceSn);
    }
}