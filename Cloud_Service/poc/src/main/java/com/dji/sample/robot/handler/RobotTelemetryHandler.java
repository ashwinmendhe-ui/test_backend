package com.dji.sample.robot.handler;

import com.dji.sample.robot.entity.RobotTelemetryData;
import com.dji.sample.robot.service.RobotWebSocketPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RobotTelemetryHandler {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final RobotWebSocketPublisher webSocketPublisher;

    public void handle(String deviceSn, String payload) {
        try {
            RobotTelemetryData data = objectMapper.readValue(payload, RobotTelemetryData.class);

            String key = "robot:" + deviceSn + ":telemetry";
            redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(data));

            webSocketPublisher.publishTelemetry(deviceSn, data);
            webSocketPublisher.publishDashboardRefresh(deviceSn, "robot-telemetry");

            log.info("Telemetry stored and pushed. deviceSn={}, key={}, data={}", deviceSn, key, data);

        } catch (Exception e) {
            log.error("Failed to handle telemetry. deviceSn={}, payload={}", deviceSn, payload, e);
        }
    }
}