package com.dji.sample.robot.handler;

import com.dji.sample.robot.entity.RobotJobStateData;
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

    public void handle(String deviceSn, String payload) {
        try {
            RobotJobStateData jobState =
                    objectMapper.readValue(payload, RobotJobStateData.class);

            String jobKey = "robot:" + deviceSn + ":jobId";
            String statusKey = "robot:" + deviceSn + ":status";
            String missionKey = "robot:" + deviceSn + ":missionId";

            if (jobState.getJobId() != null) {
                stringRedisTemplate.opsForValue().set(jobKey, jobState.getJobId());
            }

            if (jobState.getStatus() != null) {
                stringRedisTemplate.opsForValue().set(statusKey, jobState.getStatus());
            }

            if (jobState.getMissionId() != null) {
                stringRedisTemplate.opsForValue().set(missionKey, jobState.getMissionId());
            }

            log.info("Robot job state received. deviceSn={}, jobId={}, status={}, missionId={}, message={}",
                    deviceSn,
                    jobState.getJobId(),
                    jobState.getStatus(),
                    jobState.getMissionId(),
                    jobState.getMessage());

        } catch (Exception e) {
            log.error("Failed to handle robot job state. deviceSn={}, payload={}", deviceSn, payload, e);
        }
    }
}