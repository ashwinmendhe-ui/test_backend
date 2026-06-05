package com.dji.sample.robot.dto.response;

import com.dji.sample.robot.dto.response.RobotResponseData;
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
            RobotResponseData response =
                    objectMapper.readValue(payload, RobotResponseData.class);

            log.info("Robot response received. deviceSn={}, commandId={}, status={}, jobId={}, message={}",
                    deviceSn,
                    response.getCommandId(),
                    response.getStatus(),
                    response.getJobId(),
                    response.getMessage());

        } catch (Exception e) {
            log.error("Failed to handle robot response. deviceSn={}, payload={}", deviceSn, payload, e);
        }
    }
}