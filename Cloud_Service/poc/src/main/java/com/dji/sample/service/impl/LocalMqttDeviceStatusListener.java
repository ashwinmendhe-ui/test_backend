package com.dji.sample.service.impl;

import com.dji.sample.entity.Device;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.service.IDeviceRedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import com.dji.sample.robot.handler.RobotHealthHandler;
import com.dji.sample.robot.handler.RobotJobStateHandler;
import com.dji.sample.robot.handler.RobotTelemetryHandler;

// @Service
@RequiredArgsConstructor
@Slf4j
public class LocalMqttDeviceStatusListener {

    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final IDeviceRedisService deviceRedisService;
    private final RobotHealthHandler robotHealthHandler;
    private final RobotJobStateHandler robotJobStateHandler;
    private final RobotTelemetryHandler robotTelemetryHandler;

    @ServiceActivator(inputChannel = "deviceStatusMqttInputChannel")
    public void handleMessage(Message<?> message) {
        try {
            String topic = String.valueOf(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
            String payload = String.valueOf(message.getPayload());

            log.info("[MQTT] Received topic={}, payload={}", topic, payload);

            if (topic.startsWith("sys/product/") && topic.endsWith("/status")) {
                handleDjiStatus(topic, payload);
                return;
            }

            if (topic.startsWith("robot/") && topic.endsWith("/health")) {
                handleRobotHealth(topic, payload);
                return;
            }

            if (topic.startsWith("robot/") && topic.endsWith("/telemetry")) {
                String deviceSn = extractRobotDeviceSn(topic, "/telemetry");
                robotTelemetryHandler.handle(deviceSn, payload);
                return;
            }

            if (topic.startsWith("robot/") && topic.endsWith("/job/state")) {
                String deviceSn = extractRobotDeviceSn(topic, "/job/state");
                robotJobStateHandler.handle(deviceSn, payload);
                return;
            }

            if (topic.startsWith("robot/") && topic.endsWith("/state")) {
                String deviceSn = extractRobotDeviceSn(topic, "/state");
                robotJobStateHandler.handle(deviceSn, payload);
                return;
}

            log.warn("[MQTT] Unsupported topic: {}", topic);

        } catch (Exception e) {
            log.error("[MQTT] Failed to handle message", e);
        }
    }

    private void handleDjiStatus(String topic, String payload) throws Exception {
        String deviceSn = extractDjiDeviceSn(topic);

        JsonNode root = objectMapper.readTree(payload);
        JsonNode subDevices = root.path("data").path("sub_devices");

        boolean online = subDevices.isArray() && !subDevices.isEmpty();

        if (online) {
            markOnline(deviceSn, "[MQTT][DJI]");
        } else {
            markOffline(deviceSn, "[MQTT][DJI]");
        }
    }

    private void handleRobotHealth(String topic, String payload) throws Exception {
        JsonNode root = objectMapper.readTree(payload);

        String deviceSn = root.path("robot_id").asText(null);

        if (deviceSn == null || deviceSn.isBlank()) {
            deviceSn = extractRobotDeviceSn(topic, "/health");
        }

        boolean online = root.path("data").path("online").asBoolean(false);

        if (online) {
            markOnline(deviceSn, "[MQTT][ROBOT]");
        } else {
            markOffline(deviceSn, "[MQTT][ROBOT]");
        }
    }

    private void markOnline(String deviceSn, String logPrefix) {
        Device device = deviceRepository.findByDeviceSnAndDeletedAtIsNull(deviceSn)
                .orElseThrow(() -> new RuntimeException("Device not found: " + deviceSn));

        deviceRedisService.setDeviceOnline(device);
        log.info("{} Device online: {}", logPrefix, deviceSn);
    }

    private void markOffline(String deviceSn, String logPrefix) {
        deviceRedisService.delDeviceOnline(deviceSn);
        log.info("{} Device offline: {}", logPrefix, deviceSn);
    }

    private String extractDjiDeviceSn(String topic) {
        String prefix = "sys/product/";
        String suffix = "/status";

        if (!topic.startsWith(prefix) || !topic.endsWith(suffix)) {
            throw new IllegalArgumentException("Invalid DJI status topic: " + topic);
        }

        return topic.substring(prefix.length(), topic.length() - suffix.length());
    }

    private String extractRobotDeviceSn(String topic, String suffix) {
        String prefix = "robot/";

        if (!topic.startsWith(prefix) || !topic.endsWith(suffix)) {
            throw new IllegalArgumentException("Invalid robot topic: " + topic);
        }

        return topic.substring(prefix.length(), topic.length() - suffix.length());
    }
}