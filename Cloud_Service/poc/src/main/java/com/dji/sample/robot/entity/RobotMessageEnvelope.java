package com.dji.sample.robot.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RobotMessageEnvelope<T> {

    @JsonProperty("schema")
    private String schema;

    @JsonProperty("msg_id")
    private String msgId;

    @JsonProperty("robot_id")
    private String deviceSn;

    @JsonProperty("timestamp")
    private String timestamp;

    @JsonProperty("data")
    private T data;
}