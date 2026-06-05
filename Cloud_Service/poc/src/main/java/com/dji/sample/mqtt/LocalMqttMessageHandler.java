package com.dji.sample.mqtt;

import com.dji.sample.config.LocalMqttConfig;
import com.dji.sample.robot.dto.response.RobotResponseHandler;
import com.dji.sample.robot.handler.RobotJobStateHandler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;
import com.dji.sample.robot.handler.RobotJobStateHandler;

@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMqttMessageHandler {

    private final RobotResponseHandler robotResponseHandler;
    private final RobotJobStateHandler robotJobStateHandler;

    @ServiceActivator(inputChannel = LocalMqttConfig.DEVICE_STATUS_CHANNEL)
    public void handle(Message<?> message) {
        String topic = message.getHeaders().get(MqttHeaders.RECEIVED_TOPIC, String.class);
        String payload = String.valueOf(message.getPayload());

        log.info("MQTT message received. topic={}, payload={}", topic, payload);

        if (topic != null && topic.startsWith("robot/") && topic.endsWith("/response")) {
            String deviceSn = topic.split("/")[1];
            robotResponseHandler.handle(deviceSn, payload);
            return;
        }
        if (topic != null && topic.startsWith("robot/") && topic.endsWith("/job/state")) {
            String deviceSn = topic.split("/")[1];
            robotJobStateHandler.handle(deviceSn, payload);
            return;
        }

        // Keep existing health/status logic here later if needed.
    }
}