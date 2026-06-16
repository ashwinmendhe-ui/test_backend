package com.dji.sample.service.impl;

import com.dji.sample.entity.Device;
import com.dji.sample.redis.RedisConst;
import com.dji.sample.service.IDeviceRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DeviceRedisServiceImpl implements IDeviceRedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    @Value("${spring.redis.device-alive-second:84600}")
    private Long deviceAliveSecond;

    @Override
    public Boolean checkDeviceOnline(String sn) {
        String key = RedisConst.DEVICE_ONLINE_PREFIX + sn;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    @Override
    public Boolean delDeviceOnline(String sn) {
        String key = RedisConst.DEVICE_ONLINE_PREFIX + sn;
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    @Override
    public void setDeviceOnline(Device device) {
        String key = RedisConst.DEVICE_ONLINE_PREFIX + device.getDeviceSn();

        redisTemplate.opsForValue().set(
                key,
                device.getDeviceSn(),
                deviceAliveSecond,
                TimeUnit.SECONDS
        );
    }


    @Override
    public void setDeviceOnlineBySn(String deviceSn) {
        String key = RedisConst.DEVICE_ONLINE_PREFIX + deviceSn;

        redisTemplate.opsForValue().set(
                key,
                deviceSn,
                deviceAliveSecond,
                TimeUnit.SECONDS
        );
    }

    @Override
    public void setDeviceStatus(String deviceSn, String status) {
        stringRedisTemplate.opsForValue()
                .set("robot:" + deviceSn + ":status", status);
    }

    @Override
    public void setDeviceTelemetry(String deviceSn, String telemetryJson) {
        stringRedisTemplate.opsForValue()
                .set("robot:" + deviceSn + ":telemetry", telemetryJson);
    }

    @Override
    public String getRobotTelemetry(String deviceSn) {
        return stringRedisTemplate.opsForValue()
                .get("robot:" + deviceSn + ":telemetry");
    }

    @Override
    public void clearRobotJobState(String deviceSn) {
        redisTemplate.delete("robot:" + deviceSn + ":jobId");
        redisTemplate.delete("robot:" + deviceSn + ":status");
        redisTemplate.delete("robot:" + deviceSn + ":missionId");
    }
    @Override
    public void clearDeviceStatus(String deviceSn) {
        redisTemplate.delete("status:" + deviceSn);
        redisTemplate.delete("robot:" + deviceSn + ":status");
    }



    @Override
    public void setRobotJobState(
            String deviceSn,
            String jobId,
            String status,
            String missionId
    ) {
        stringRedisTemplate.opsForValue().set("robot:" + deviceSn + ":jobId", jobId);
        stringRedisTemplate.opsForValue().set("robot:" + deviceSn + ":status", status);
        stringRedisTemplate.opsForValue().set("robot:" + deviceSn + ":missionId", missionId);
    }

    @Override
    public Device getDeviceOnline(String sn) {
        String key = RedisConst.DEVICE_ONLINE_PREFIX + sn;

        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            Device device = new Device();
            device.setDeviceSn(sn);
            return device;
        }

        return null;
    }
}