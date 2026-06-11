package com.dji.sample.robot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class RobotCommandData {

    @JsonProperty("command_id")
    private String commandId;

    @JsonProperty("action")
    private String action;

    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("payload")
    private Map<String, Object> payload;
}