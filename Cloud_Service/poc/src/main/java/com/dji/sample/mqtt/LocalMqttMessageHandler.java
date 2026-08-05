package com.dji.sample.mqtt;

import com.dji.sample.config.LocalMqttConfig;
import com.dji.sample.repository.DeviceRepository;
import com.dji.sample.robot.dto.response.RobotResponseHandler;
import com.dji.sample.robot.handler.RobotHealthHandler;
import com.dji.sample.robot.handler.RobotJobStateHandler;
import com.dji.sample.robot.handler.RobotTelemetryHandler;
import com.dji.sample.service.IDeviceRedisService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import com.dji.sdk.mqtt.services.ServicesReplyHandler;
import org.springframework.messaging.support.MessageBuilder;
import com.dji.sample.service.DeviceWebSocketPublisher;
import com.dji.sdk.mqtt.ChannelName;
import org.springframework.messaging.MessageChannel;
import jakarta.annotation.Resource;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMqttMessageHandler {

    private final RobotResponseHandler robotResponseHandler;
    private final RobotJobStateHandler robotJobStateHandler;
    private final RobotTelemetryHandler robotTelemetryHandler;
    private final RobotHealthHandler robotHealthHandler;
    private final ObjectMapper objectMapper;
    private final DeviceRepository deviceRepository;
    private final IDeviceRedisService deviceRedisService;
    private final ServicesReplyHandler servicesReplyHandler;
    private final DeviceWebSocketPublisher webSocketPublisher;

    @Resource(name = ChannelName.INBOUND_STATUS)
    private MessageChannel djiInboundStatusChannel;

    @ServiceActivator(inputChannel = LocalMqttConfig.DEVICE_STATUS_CHANNEL)
    public void handle(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = String.valueOf(message.getPayload());

        if (topic == null || topic.isBlank()) {
            log.warn("MQTT topic is missing. payload={}", payload);
            return;
        }

        log.info("MQTT message received. topic={}, payload={}", topic, payload);

        try {

            if (topic.startsWith("thing/product/") && topic.endsWith("/services_reply")) {
                Message<byte[]> sdkMessage = MessageBuilder
                .withPayload(payload.getBytes(StandardCharsets.UTF_8))
                .copyHeaders(message.getHeaders())
                .build();

            servicesReplyHandler.servicesReply(sdkMessage);
                log.info("[MQTT][DJI][SERVICES_REPLY] Forwarded to SDK handler. topic={}", topic);
                return;
            }

            if (topic.startsWith("thing/product/") && topic.endsWith("/osd")) {
                handleDjiOsd(topic, payload);
                return;
            }
            if (topic.startsWith("sys/product/") && topic.endsWith("/status")) {
                Message<byte[]> sdkMessage = MessageBuilder
                        .withPayload(payload.getBytes(StandardCharsets.UTF_8))
                        .copyHeaders(message.getHeaders())
                        .build();

                boolean sent = djiInboundStatusChannel.send(sdkMessage);

                log.info(
                        "[MQTT][DJI][STATUS] Forwarded to SDK StatusRouter. topic={}, sent={}",
                        topic,
                        sent
                );

                return;
            }

            if (topic.startsWith("robot/") && topic.endsWith("/response")) {
                robotResponseHandler.handle(topic.split("/")[1], payload);
                return;
            }

            if (topic.startsWith("robot/") && topic.endsWith("/health")) {
                robotHealthHandler.handle(topic.split("/")[1], payload);
                return;
            }

            if (topic.startsWith("robot/") && topic.endsWith("/job/state")) {
                robotJobStateHandler.handle(topic.split("/")[1], payload);
                return;
            }

            if (topic.startsWith("robot/") && topic.endsWith("/state")) {
                robotJobStateHandler.handle(topic.split("/")[1], payload);
                return;
            }

            if (topic.matches("robot/[^/]+/telemetry")) {
                robotTelemetryHandler.handle(topic.split("/")[1], payload);
                return;
            }

            log.debug("Unhandled MQTT topic. topic={}, payload={}", topic, payload);

        } catch (Exception e) {
            log.error("Failed to handle MQTT message. topic={}, payload={}", topic, payload, e);
        }
    }


    private void handleDjiOsd(String topic, String payload) throws Exception {
        String topicDeviceSn = topic.substring("thing/product/".length(), topic.length() - "/osd".length());

        JsonNode root = objectMapper.readTree(payload);
        JsonNode data = root.path("data");

        // For DJI, gateway can be RC/gateway SN. Prefer gateway if it exists in local DB.
        String gatewaySn = root.path("gateway").asText(null);
        String deviceSn = topicDeviceSn;

        if (gatewaySn != null && !gatewaySn.isBlank()
                && deviceRepository.findByDeviceSnAndDeletedAtIsNull(gatewaySn).isPresent()) {
            deviceSn = gatewaySn;
        }

        Map<String, Object> telemetry = new HashMap<>();
        telemetry.put("deviceSn", deviceSn);
        telemetry.put("sourceDeviceSn", topicDeviceSn);
        telemetry.put("gateway", gatewaySn);
        telemetry.put("deviceType", "Drone");
        telemetry.put("status", "online");
        telemetry.put("battery", extractBattery(data));
        telemetry.put("altitude", data.path("height").asDouble(data.path("elevation").asDouble(0)));
        telemetry.put("latitude", data.path("latitude").asDouble(0));
        telemetry.put("longitude", data.path("longitude").asDouble(0));
        telemetry.put("speed", data.path("horizontal_speed").asDouble(0));
        telemetry.put("network", extractNetwork(data));
        telemetry.put("timestamp", root.path("timestamp").asLong(System.currentTimeMillis()));

        String telemetryJson = objectMapper.writeValueAsString(telemetry);

        deviceRedisService.setDeviceOnlineBySn(deviceSn, 120);
        deviceRedisService.setDeviceTelemetry(deviceSn, telemetryJson);

        webSocketPublisher.publishDashboardRefresh(deviceSn, "dji-osd");
        log.info("[MQTT][DJI][OSD][WS] Dashboard refresh published. deviceSn={}", deviceSn);

        log.info("[MQTT][DJI][OSD] Drone telemetry updated. deviceSn={}, sourceDeviceSn={}", deviceSn, topicDeviceSn);
    }

    private int extractBattery(JsonNode data) {
        if (data.has("capacity_percent")) {
            return data.path("capacity_percent").asInt(0);
        }
        return data.path("battery").path("capacity_percent").asInt(0);
    }

    private String extractNetwork(JsonNode data) {
        JsonNode wireless = data.path("wireless_link");

        if (!wireless.isMissingNode()) {
            int sdrQuality = wireless.path("sdr_quality").asInt(0);
            int fourGQuality = wireless.path("4g_quality").asInt(0);

            if (sdrQuality > 0) {
                return "SDR";
            }
            if (fourGQuality > 0) {
                return "4G";
            }
            return "CONNECTED";
        }

        return "UNKNOWN";
    }

    }