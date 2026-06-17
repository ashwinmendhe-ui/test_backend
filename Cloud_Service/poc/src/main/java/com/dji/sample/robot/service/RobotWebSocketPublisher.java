package com.dji.sample.robot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RobotWebSocketPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishTelemetry(String deviceSn, Object telemetry) {
        messagingTemplate.convertAndSend(
                "/topic/robot/" + deviceSn + "/telemetry",
                telemetry
        );
    }

    public void publishStatus(String deviceSn, Object status) {
        messagingTemplate.convertAndSend(
                "/topic/robot/" + deviceSn + "/status",
                status
        );
    }

    public void publishDashboardRefresh(String deviceSn, String source) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "DEVICE_REFRESH");
        payload.put("deviceSn", deviceSn);
        payload.put("source", source);
        payload.put("timestamp", Instant.now().toString());

        log.info("[WS][Dashboard] Publishing device refresh. deviceSn={}, source={}", deviceSn, source);
        messagingTemplate.convertAndSend("/topic/dashboard/devices", payload);
    }

    public void publishDashboardStatus(String deviceSn, String status, String source) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("eventType", "DEVICE_STATUS_CHANGED");
        payload.put("deviceSn", deviceSn);
        payload.put("status", status);
        payload.put("source", source);
        payload.put("timestamp", Instant.now().toString());

        log.info("[WS][Dashboard] Publishing device status. deviceSn={}, status={}, source={}", deviceSn, status, source);
        messagingTemplate.convertAndSend("/topic/dashboard/devices", payload);
    }
}