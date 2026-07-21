package com.dji.sample.robot.handler;

import com.dji.sample.robot.entity.RobotJobStateData;
import com.dji.sample.service.DeviceWebSocketPublisher;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.Set;
import com.dji.sample.repository.LiveStreamSessionRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotJobStateHandler {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final DeviceWebSocketPublisher webSocketPublisher;
    private final LiveStreamSessionRepository liveStreamSessionRepository;

    public void handle(String deviceSn, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.has("data") ? root.path("data") : root;

            String jobId = firstText(data, "job_id", "jobId");
            String status = firstText(data, "status", "job_status", "jobStatus");
            String missionId = firstText(data, "mission_id", "missionId");
            String message = textOrNull(data, "message");

            log.info(
                        "Parsed robot job state. deviceSn={}, jobId={}, status={}, missionId={}, rawData={}",
                        deviceSn,
                        jobId,
                        status,
                        missionId,
                        data
                );


            RobotJobStateData jobState = new RobotJobStateData();
            jobState.setJobId(jobId);
            jobState.setStatus(status);
            jobState.setMissionId(missionId);
            jobState.setMessage(message);

            
            String jobKey = "robot:" + deviceSn + ":jobId";
            String localStatusKey = "robot:" + deviceSn + ":status";
            String prodStatusKey = "status:" + deviceSn;
            String missionKey = "robot:" + deviceSn + ":missionId";

            Set<String> terminalStates = Set.of(
                    "COMPLETED",
                    "COMPLETE",
                    "FAILED",
                    "CANCELLED",
                    "CANCELED",
                    "STOPPED",
                    "IDLE"
            );

            if (status != null && terminalStates.contains(status.toUpperCase())) {

                stringRedisTemplate.delete(jobKey);
                stringRedisTemplate.delete(localStatusKey);
                stringRedisTemplate.delete(prodStatusKey);
                stringRedisTemplate.delete(missionKey);

                log.info("Robot job cleared. deviceSn={}, status={}", deviceSn, status);

            } else {


                boolean hasActiveSession = liveStreamSessionRepository
                        .findFirstByDeviceSnAndSessionStatusOrderByStartedAtDesc(deviceSn, "ACTIVE")
                        .isPresent();

                if (!hasActiveSession) {
                    stringRedisTemplate.delete(jobKey);
                    stringRedisTemplate.delete(localStatusKey);
                    stringRedisTemplate.delete(prodStatusKey);
                    stringRedisTemplate.delete(missionKey);

                    log.warn("Ignoring stale robot job state because no ACTIVE session exists. deviceSn={}, jobId={}, status={}",
                            deviceSn, jobId, status);
                    return;
                }

                Duration ttl = Duration.ofMinutes(10);

                if (jobId != null) {
                    stringRedisTemplate.opsForValue().set(jobKey, jobId, ttl);
                }

                if (status != null) {
                    stringRedisTemplate.opsForValue().set(localStatusKey, status, ttl);
                    stringRedisTemplate.opsForValue().set(prodStatusKey, status, ttl);
                }

                if (missionId != null) {
                    stringRedisTemplate.opsForValue().set(missionKey, missionId, ttl);
                }
            }
            webSocketPublisher.publishStatus(deviceSn, jobState);
            webSocketPublisher.publishDashboardStatus(deviceSn, status, "robot-job-state");

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

    private String firstText(JsonNode node, String... fieldNames) {
        for (String fieldName : fieldNames) {
            String value = textOrNull(node, fieldName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}