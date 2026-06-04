package com.dji.sample.service.impl;

import com.dji.sample.entity.Device;
import com.dji.sample.redis.RedisConst;
import com.dji.sample.service.IDeviceRedisService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class DeviceRedisServiceImpl implements IDeviceRedisService {

    private final RedisTemplate<String, Object> redisTemplate;

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