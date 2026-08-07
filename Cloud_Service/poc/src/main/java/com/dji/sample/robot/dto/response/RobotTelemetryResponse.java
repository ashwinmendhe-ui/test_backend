package com.dji.sample.robot.dto.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class RobotTelemetryResponse {

    private Double battery;

    private String status;

    private String network;

    private String gps;
    private String gpsFix;

    private Double speed;

    private Double latitude;
    private Double longitude;

    private Double altitude;
    private Double heading;

    private Integer rssi;
    private Integer rsrp;
    private Integer latencyMs;

    private Double voltage;
    private Boolean charging;

    private String deviceSn;
    private String sourceDeviceSn;
    private String gateway;
    private String deviceType;

    private String timestamp;
}