package com.dji.sample.robot.dto.response;

import lombok.Data;

@Data
public class RobotTelemetryResponse {
    private Integer battery;
    private Double speed;
    private Double latitude;
    private Double longitude;
    private Double heading;
    private String network;
    private String timestamp;
}