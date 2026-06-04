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
    public void handleDeviceStatus(Message<?> message) {
        try {
            String topic = String.valueOf(message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC));
            String payload = String.valueOf(message.getPayload());

            String deviceSn = extractDeviceSn(topic);

            JsonNode root = objectMapper.readTree(payload);
            JsonNode subDevices = root.path("data").path("sub_devices");

            boolean online = subDevices.isArray() && !subDevices.isEmpty();

            if (online) {
                Device device = deviceRepository.findByDeviceSnAndDeletedAtIsNull(deviceSn)
                        .orElseThrow(() -> new RuntimeException("Device not found: " + deviceSn));

                deviceRedisService.setDeviceOnline(device);
                log.info("[MQTT] Device online: {}", deviceSn);
            } else {
                deviceRedisService.delDeviceOnline(deviceSn);
                log.info("[MQTT] Device offline: {}", deviceSn);
            }

        } catch (Exception e) {
            log.error("[MQTT] Failed to handle device status message", e);
        }
    }

    private String extractDeviceSn(String topic) {
        // sys/product/{sn}/status
        String prefix = "sys/product/";
        String suffix = "/status";

        if (!topic.startsWith(prefix) || !topic.endsWith(suffix)) {
            throw new IllegalArgumentException("Invalid device status topic: " + topic);
        }

        return topic.substring(prefix.length(), topic.length() - suffix.length());
    }
}