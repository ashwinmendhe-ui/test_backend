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

@Service
@RequiredArgsConstructor
@Slf4j
public class LocalMqttDeviceStatusListener {

    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final IDeviceRedisService deviceRedisService;

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
            deviceSn = extractRobotDeviceSn(topic);
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

    private String extractRobotDeviceSn(String topic) {
        String prefix = "robot/";
        String suffix = "/health";

        if (!topic.startsWith(prefix) || !topic.endsWith(suffix)) {
            throw new IllegalArgumentException("Invalid robot health topic: " + topic);
        }

        return topic.substring(prefix.length(), topic.length() - suffix.length());
    }
}