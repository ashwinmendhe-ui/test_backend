package com.dji.sample.robot.entity;

import lombok.Data;

@Data
public class RobotTelemetryData {
    private Integer battery;
    private Double speed;
    private Double latitude;
    private Double longitude;
    private Double heading;
    private String network;
    private String timestamp;
}