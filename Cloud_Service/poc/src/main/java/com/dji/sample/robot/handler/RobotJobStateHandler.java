package com.dji.sample.robot.handler;

import com.dji.sample.robot.entity.RobotJobStateData;
import com.dji.sample.robot.service.RobotWebSocketPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotJobStateHandler {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RobotWebSocketPublisher webSocketPublisher;

    public void handle(String deviceSn, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.has("data") ? root.path("data") : root;

            String jobId = textOrNull(data, "job_id");
            String status = textOrNull(data, "status");
            String missionId = textOrNull(data, "mission_id");
            String message = textOrNull(data, "message");

            RobotJobStateData jobState = new RobotJobStateData();
            jobState.setJobId(jobId);
            jobState.setStatus(status);
            jobState.setMissionId(missionId);
            jobState.setMessage(message);

            String jobKey = "robot:" + deviceSn + ":jobId";
            String localStatusKey = "robot:" + deviceSn + ":status";
            String prodStatusKey = "status:" + deviceSn;
            String missionKey = "robot:" + deviceSn + ":missionId";

            if (jobId != null) {
                stringRedisTemplate.opsForValue().set(jobKey, jobId);
            }

            if (status != null) {
                stringRedisTemplate.opsForValue().set(localStatusKey, status);
                stringRedisTemplate.opsForValue().set(prodStatusKey, status);
            }

            if (missionId != null) {
                stringRedisTemplate.opsForValue().set(missionKey, missionId);
            }

            webSocketPublisher.publishStatus(deviceSn, jobState);

            log.info("Robot job state received. deviceSn={}, jobId={}, status={}, missionId={}, message={}",
                    deviceSn, jobId, status, missionId, message);

        } catch (Exception e) {
            log.error("Failed to handle robot job state. deviceSn={}, payload={}", deviceSn, payload, e);
        }
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() || "null".equalsIgnoreCase(text) ? null : text;
    }
}