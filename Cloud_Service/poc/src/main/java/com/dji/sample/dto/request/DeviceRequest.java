package com.dji.sample.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class DeviceRequest {

    private UUID deviceId;

    private UUID companyId;

    private UUID siteId;

    private String deviceName;

    private String deviceType;

    private String brandName;

    private String model;

    private String deviceSn;

    private String description;
}