package com.dji.sample.robot.dto.response;

import lombok.Data;

@Data
public class RobotResponseData {

    private String commandId;
    private String status;   // ACK / FAILED
    private String jobId;
    private String message;
}