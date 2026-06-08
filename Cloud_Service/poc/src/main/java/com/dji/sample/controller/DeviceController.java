package com.dji.sample.controller;

import com.dji.sample.dto.request.DeviceRequest;
import com.dji.sample.dto.response.DeviceResponse;
import com.dji.sample.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import com.dji.sample.dto.response.ApiResponse;
import com.dji.sample.robot.dto.response.RobotTelemetryResponse;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<DeviceResponse> getDevices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to,
            @RequestParam(required = false) UUID siteId
    ) {
        return deviceService.getDevices(keyword, from, to, siteId);
    }

    @GetMapping("/{deviceId}")
    public DeviceResponse getDevice(@PathVariable UUID deviceId) {
        return deviceService.getDevice(deviceId);
    }


    @GetMapping("/{deviceSn}/telemetry")
    public ApiResponse<RobotTelemetryResponse> getDeviceTelemetry(
            @PathVariable String deviceSn
    ) {
        return ApiResponse.<RobotTelemetryResponse>builder()
                .success(true)
                .message("Device telemetry fetched successfully")
                .data(deviceService.getTelemetryByDeviceSn(deviceSn))
                .build();
    }

    @PostMapping
    public DeviceResponse createDevice(@RequestBody DeviceRequest request) {
        return deviceService.createDevice(request);
    }

    @PostMapping("/update/{deviceId}")
    public DeviceResponse updateDevice(
            @PathVariable UUID deviceId,
            @RequestBody DeviceRequest request
    ) {
        return deviceService.updateDevice(deviceId, request);
    }

    @PostMapping("/delete/{deviceId}")
    public void deleteDevice(@PathVariable UUID deviceId) {
        deviceService.deleteDevice(deviceId);
    }

    @PostMapping("/{deviceSn}/online-test")
    public ApiResponse<String> markDeviceOnline(@PathVariable String deviceSn) {
        deviceService.markDeviceOnlineForTest(deviceSn);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Device marked online")
                .data(deviceSn)
                .build();
    }

    @PostMapping("/{deviceSn}/offline-test")
    public ApiResponse<String> markDeviceOffline(@PathVariable String deviceSn) {
        deviceService.markDeviceOfflineForTest(deviceSn);

        return ApiResponse.<String>builder()
                .success(true)
                .message("Device marked offline")
                .data(deviceSn)
                .build();
    }
}