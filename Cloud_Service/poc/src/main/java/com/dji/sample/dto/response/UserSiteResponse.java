package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class UserSiteResponse {

    private UUID siteId;

    private String siteName;

    private List<UUID> missionList;

    private List<UUID> deviceList;
    private String createdAt;
}