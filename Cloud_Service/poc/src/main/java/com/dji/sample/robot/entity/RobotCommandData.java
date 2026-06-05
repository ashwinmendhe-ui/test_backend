package com.dji.sample.robot.entity;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RobotCommandData {

    private String commandId;
    private String action;
    private String jobId;
    private Map<String, Object> payload;
}