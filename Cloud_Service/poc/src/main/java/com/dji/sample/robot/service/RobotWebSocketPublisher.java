package com.dji.sample.robot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
}