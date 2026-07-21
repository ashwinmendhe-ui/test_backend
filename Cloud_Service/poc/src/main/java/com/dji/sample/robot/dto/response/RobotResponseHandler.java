package com.dji.sample.robot.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.data.redis.core.StringRedisTemplate;
import java.time.Duration;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotResponseHandler {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;

    public void handle(String deviceSn, String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode data = root.has("data")
                    ? root.path("data")
                    : root;

            String commandId = textOrNull(data, "command_id");
            String result = textOrNull(data, "result");
            String reason = textOrNull(data, "reason");
            String jobId = textOrNull(data, "job_id");
            String jobStatus = textOrNull(data, "job_status");

            log.info(
                    "Robot response received. deviceSn={}, commandId={}, result={}, reason={}, jobId={}, jobStatus={}",
                    deviceSn,
                    commandId,
                    result,
                    reason,
                    jobId,
                    jobStatus
            );

            Set<String> activeJobReasons = Set.of(
                    "JOB_ID_MISMATCH",
                    "JOB_ALREADY_EXISTS"
            );

            if ("rejected".equalsIgnoreCase(result)
                    && reason != null
                    && activeJobReasons.contains(reason.toUpperCase())
                    && jobId != null
                    && jobStatus != null
                    && "RUNNING".equalsIgnoreCase(jobStatus)) {

                String jobKey = "robot:" + deviceSn + ":jobId";

                stringRedisTemplate.opsForValue().set(
                        jobKey,
                        jobId,
                        Duration.ofMinutes(10)
                );

                log.warn(
                        "Robot active jobId synchronized from command response. deviceSn={}, reason={}, jobId={}, jobStatus={}",
                        deviceSn,
                        reason,
                        jobId,
                        jobStatus
                );
            }

            if ("rejected".equalsIgnoreCase(result)) {
                log.warn(
                        "Robot command rejected. deviceSn={}, commandId={}, reason={}, jobId={}, jobStatus={}",
                        deviceSn,
                        commandId,
                        reason,
                        jobId,
                        jobStatus
                );
            }

        } catch (Exception e) {
            log.error(
                    "Failed to handle robot response. deviceSn={}, payload={}",
                    deviceSn,
                    payload,
                    e
            );
        }
    }

    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);

        if (value.isMissingNode() || value.isNull()) {
            return null;
        }

        String text = value.asText();

        if (text == null
                || text.isBlank()
                || "null".equalsIgnoreCase(text)) {
            return null;
        }

        return text;
    }
}