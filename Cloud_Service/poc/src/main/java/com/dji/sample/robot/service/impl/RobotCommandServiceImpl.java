package com.dji.sample.robot.service.impl;
import com.dji.sample.robot.service.IRobotCommandService;
import com.dji.sample.robot.entity.RobotCommandData;
import com.dji.sample.robot.entity.RobotMessageEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RobotCommandServiceImpl implements IRobotCommandService {

    private final MessageChannel outboundRobotCommand;
    private final ObjectMapper objectMapper;

    @Override
    public void createJob(String robotId, String commandId, String jobId, Object payload) {
        RobotCommandData commandData = RobotCommandData.builder()
                .commandId(commandId)
                .action("create_job")
                .jobId(jobId)
                .payload(payload instanceof Map ? (Map<String, Object>) payload : convertToMap(payload))
                .build();

        sendCommand(robotId, commandData);
    }

    @Override
    public void startJob(String robotId, String commandId, String jobId) {
        RobotCommandData commandData = RobotCommandData.builder()
                .commandId(commandId)
                .action("start_job")
                .jobId(jobId)
                .payload(new HashMap<>())
                .build();

        sendCommand(robotId, commandData);
    }

    @Override
    public void ackJob(String robotId, String commandId, String jobId) {
        RobotCommandData commandData = RobotCommandData.builder()
                .commandId(commandId)
                .action("ack_job")
                .jobId(jobId)
                .payload(new HashMap<>())
                .build();

        sendCommand(robotId, commandData);
    }

    @Override
    public void cancelJob(String robotId, String commandId, String jobId) {
        RobotCommandData commandData = RobotCommandData.builder()
                .commandId(commandId)
                .action("cancel_job")
                .jobId(jobId)
                .payload(new HashMap<>())
                .build();

        sendCommand(robotId, commandData);
    }

    @Override
    public void cleanJob(String robotId) {
        String commandId = UUID.randomUUID().toString();

        RobotCommandData commandData = RobotCommandData.builder()
                .commandId(commandId)
                .action("clean_job")
                .jobId(null)
                .payload(new HashMap<>())
                .build();

        sendCommand(robotId, commandData);
    }

    @Override
    public void sendCommand(String robotId, RobotCommandData commandData) {
        try {
            RobotMessageEnvelope<RobotCommandData> envelope =
                    RobotMessageEnvelope.<RobotCommandData>builder()
                            .schema("robot-mqtt.v1")
                            .msgId(UUID.randomUUID().toString())
                            .deviceSn(robotId)
                            .timestamp(Instant.now().toString())
                            .data(commandData)
                            .build();

            String topic = "robot/" + robotId + "/command";
            String payload = objectMapper.writeValueAsString(envelope);

            outboundRobotCommand.send(
                    MessageBuilder.withPayload(payload)
                            .setHeader(MqttHeaders.TOPIC, topic)
                            .setHeader(MqttHeaders.QOS, 1)
                            .setHeader(MqttHeaders.RETAINED, false)
                            .build()
            );

            log.info("Robot command sent. robotId={}, action={}, commandId={}, jobId={}, topic={}",
                    robotId,
                    commandData.getAction(),
                    commandData.getCommandId(),
                    commandData.getJobId(),
                    topic);

        } catch (Exception e) {
            log.error("Failed to send robot command. robotId={}", robotId, e);
            throw new RuntimeException("Failed to send robot command", e);
        }
    }

    private Map<String, Object> convertToMap(Object obj) {
        if (obj == null) {
            return new HashMap<>();
        }
        return objectMapper.convertValue(obj, Map.class);
    }
}