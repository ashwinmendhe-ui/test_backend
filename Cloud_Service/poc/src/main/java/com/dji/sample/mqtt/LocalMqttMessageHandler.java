package com.dji.sample.mqtt;

import com.dji.sample.config.LocalMqttConfig;
import com.dji.sample.robot.dto.response.RobotResponseHandler;
import com.dji.sample.robot.handler.RobotJobStateHandler;
import com.dji.sample.robot.handler.RobotTelemetryHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import com.dji.sample.robot.handler.RobotHealthHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMqttMessageHandler {

    private final RobotResponseHandler robotResponseHandler;
    private final RobotJobStateHandler robotJobStateHandler;
    private final RobotTelemetryHandler robotTelemetryHandler;
    private final RobotHealthHandler robotHealthHandler;
    @ServiceActivator(inputChannel = LocalMqttConfig.DEVICE_STATUS_CHANNEL)
    public void handle(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = String.valueOf(message.getPayload());

        if (topic == null || topic.isBlank()) {
            log.warn("MQTT topic is missing. payload={}", payload);
            return;
        }

        log.info("MQTT message received. topic={}, payload={}", topic, payload);

        if (topic.startsWith("robot/") && topic.endsWith("/response")) {
            String deviceSn = topic.split("/")[1];
            robotResponseHandler.handle(deviceSn, payload);
            return;
        }

        if (topic.startsWith("robot/") && topic.endsWith("/health")) {
            String deviceSn = topic.split("/")[1];
            robotHealthHandler.handle(deviceSn, payload);
            return;
        }

        if (topic.startsWith("robot/") && topic.endsWith("/job/state")) {
            String deviceSn = topic.split("/")[1];
            robotJobStateHandler.handle(deviceSn, payload);
            return;
        }

        if (topic.matches("robot/[^/]+/telemetry")) {
            String deviceSn = topic.split("/")[1];
            robotTelemetryHandler.handle(deviceSn, payload);
            return;
        }

        log.debug("Unhandled MQTT topic. topic={}, payload={}", topic, payload);
    }
}