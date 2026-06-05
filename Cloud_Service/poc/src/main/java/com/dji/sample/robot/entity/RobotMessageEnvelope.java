package com.dji.sample.robot.entity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RobotMessageEnvelope<T> {

    private String schema;
    private String msgId;
    private String deviceSn;
    private String timestamp;
    private T data;
}