package com.dji.sample.drone.model;

public record StreamSource(
        String requestDeviceSn,
        String streamDeviceSn,
        String gatewaySn,
        String droneSn,
        String payloadIndex,
        String videoType,
        String videoId
) {
}