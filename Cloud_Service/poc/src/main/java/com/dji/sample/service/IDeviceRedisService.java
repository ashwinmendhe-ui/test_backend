package com.dji.sample.service;

import com.dji.sample.entity.Device;

public interface IDeviceRedisService {

    Boolean checkDeviceOnline(String sn);

    Boolean delDeviceOnline(String sn);

    void setDeviceOnline(Device device);

    Device getDeviceOnline(String sn);
}