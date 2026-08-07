package com.dji.sample.service;

import java.util.Map;

public interface DeviceTelemetryHistoryService {

    void recordTelemetry(
            String deviceSn,
            Map<String, Object> telemetry
    );
}