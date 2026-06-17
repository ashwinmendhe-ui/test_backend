package com.dji.sample.service;

import com.dji.sample.entity.Device;

public interface IDeviceRedisService {

    Boolean checkDeviceOnline(String sn);

    Boolean delDeviceOnline(String sn); 

    void setDeviceOnline(Device device);

    void setDeviceOnlineBySn(String deviceSn);

    Device getDeviceOnline(String sn);

    String getRobotTelemetry(String deviceSn);

    void setDeviceStatus(String deviceSn, String status);
    void setDeviceOnlineBySn(String deviceSn, long ttlSeconds);

    void setDeviceTelemetry(String deviceSn, String telemetryJson);

    void clearRobotJobState(String deviceSn);

    void clearDeviceStatus(String deviceSn);

    void setRobotJobState(
            String deviceSn,
            String jobId,
            String status,
            String missionId
    );

    void clearAllRobotJobState();
}