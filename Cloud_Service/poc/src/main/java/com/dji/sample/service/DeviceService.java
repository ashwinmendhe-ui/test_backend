package com.dji.sample.service;

import com.dji.sample.dto.request.DeviceRequest;
import com.dji.sample.dto.response.DeviceResponse;
import com.dji.sample.robot.dto.response.RobotTelemetryResponse;

import java.util.List;
import java.util.UUID;

public interface DeviceService {

    List<DeviceResponse> getDevices(
            String keyword,
            String from,
            String to,
            UUID siteId
    );

    DeviceResponse getDevice(UUID deviceId);

    DeviceResponse createDevice(DeviceRequest request);

    DeviceResponse updateDevice(UUID deviceId, DeviceRequest request);

    void deleteDevice(UUID deviceId);

    RobotTelemetryResponse getTelemetryByDeviceSn(String deviceSn);
}