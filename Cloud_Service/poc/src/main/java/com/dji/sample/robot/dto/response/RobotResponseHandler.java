package com.dji.sample.robot.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotResponseHandler {

    private final ObjectMapper objectMapper;

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