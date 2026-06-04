package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class SiteResponse {

    private UUID siteId;

    private UUID companyId;

    private String companyName;

    private String name;

    private String siteName;

    private String address;

    private String description;

    private Boolean isActive;

    private String createdAt;
    private String updatedAt;
    private String phoneNumber;

    private String email;
    private Integer deviceCount;
    private Integer deviceOnlineCount;
}