package com.dji.sample.service;

import com.dji.sample.entity.Device;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.service.DeviceWebSocketPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceOfflineMonitor {

    private final DeviceRepository deviceRepository;
    private final StringRedisTemplate redisTemplate;
    private final DeviceWebSocketPublisher webSocketPublisher;
    private final Set<String> lastOnlineDevices = ConcurrentHashMap.newKeySet();

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
        Boolean online = redisTemplate.hasKey(onlineKey);

        if (Boolean.TRUE.equals(online)) {
            lastOnlineDevices.add(deviceSn);
            return;
        }

        boolean wasOnlineBefore = lastOnlineDevices.remove(deviceSn);

        if (wasOnlineBefore) {
            log.info("[DeviceOfflineMonitor] Device offline detected. deviceSn={}", deviceSn);

            webSocketPublisher.publishDashboardStatus(
                    deviceSn,
                    "OFFLINE",
                    "device-offline-monitor"
            );
        }
    }
}