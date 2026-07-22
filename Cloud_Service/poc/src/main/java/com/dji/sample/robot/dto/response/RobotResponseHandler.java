package com.dji.sample.robot.dto.response;

import com.dji.sample.robot.service.IRobotCommandService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RobotResponseHandler {

    private final ObjectMapper objectMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final IRobotCommandService robotCommandService;

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

            /*
             * create_job was accepted and the robot created the job.
             * The job must now be explicitly started.
             */
            if ("accepted".equalsIgnoreCase(result)
                    && "PENDING".equalsIgnoreCase(jobStatus)
                    && jobId != null) {

                sendStartJobOnce(deviceSn, jobId);
            }

            /*
             * The robot rejected our command because another job is already
             * running. Synchronize the real physical jobId into Redis so that
             * cleanup/cancel uses the correct job.
             */
            Set<String> activeJobReasons = Set.of(
                    "JOB_ID_MISMATCH",
                    "JOB_ALREADY_EXISTS"
            );

            if ("rejected".equalsIgnoreCase(result)
                    && reason != null
                    && activeJobReasons.contains(reason.toUpperCase())
                    && jobId != null
                    && "RUNNING".equalsIgnoreCase(jobStatus)) {

                String jobKey = "robot:" + deviceSn + ":jobId";

                // Runtime job state should not expire while the job is active.
                stringRedisTemplate.opsForValue().set(jobKey, jobId);

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

    private void sendStartJobOnce(String deviceSn, String jobId) {
        String guardKey =
                "robot:" + deviceSn + ":startJobSent:" + jobId;

        /*
         * MQTT QoS 1 can deliver the same response more than once.
         * This short-lived guard prevents duplicate start_job commands.
         */
        Boolean firstAttempt = stringRedisTemplate
                .opsForValue()
                .setIfAbsent(
                        guardKey,
                        "1",
                        Duration.ofMinutes(2)
                );

        if (!Boolean.TRUE.equals(firstAttempt)) {
            log.debug(
                    "Skipping duplicate start_job command. deviceSn={}, jobId={}",
                    deviceSn,
                    jobId
            );
            return;
        }

        String startCommandId = UUID.randomUUID().toString();

        try {
            robotCommandService.startJob(
                    deviceSn,
                    startCommandId,
                    jobId
            );

            log.info(
                    "Robot start_job sent after create_job acceptance. deviceSn={}, commandId={}, jobId={}",
                    deviceSn,
                    startCommandId,
                    jobId
            );

        } catch (Exception e) {
            // Allow a retry if publishing failed.
            stringRedisTemplate.delete(guardKey);

            log.error(
                    "Failed to send start_job after create_job acceptance. deviceSn={}, jobId={}",
                    deviceSn,
                    jobId,
                    e
            );

            throw e;
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