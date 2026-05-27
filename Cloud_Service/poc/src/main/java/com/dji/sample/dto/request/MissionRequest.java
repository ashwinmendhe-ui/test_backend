package com.dji.sample.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class MissionRequest {

    private UUID companyId;

    private UUID siteId;

    private String missionName;

    private String missionType;

    private String deviceType;

    private String file;

    private String downloadUrl;

    private String description;
}