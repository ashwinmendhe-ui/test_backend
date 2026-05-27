package com.dji.sample.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class MissionResponse {

    private UUID missionId;

    private UUID companyId;

    private String companyName;

    private UUID siteId;

    private String siteName;

    private String missionName;

    // FE compatibility alias
    private String name;

    private String missionType;

    private String deviceType;

    private String file;

    // FE compatibility alias
    private String fileName;

    private String downloadUrl;

    private String description;
    private String id;
    private Integer code;
    private String uploadUrl;
    private String objectKey;

    @JsonFormat(
            pattern = "yyyy-MM-dd HH:mm:ss",
            timezone = "Asia/Seoul"
    )
    private OffsetDateTime createdAt;
}