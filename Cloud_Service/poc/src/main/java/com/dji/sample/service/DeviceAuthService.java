package com.dji.sample.service;

import com.dji.sample.dto.auth.DeviceLoginRequest;
import com.dji.sample.dto.auth.DeviceLoginResponse;

public interface DeviceAuthService {

    DeviceLoginResponse login(DeviceLoginRequest request);
}