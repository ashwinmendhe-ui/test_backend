package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@Builder
public class DeviceResponse {

    private UUID deviceId;

    private String deviceName;

    private UUID companyId;

    private String companyName;

    private UUID siteId;

    private String siteName;

    private String deviceType;

    private String brandName;

    private String model;

    private String deviceSn;

    private String description;

    private String status;

    private String createdAt;
    private String updatedAt;
    private String createdDate;
}