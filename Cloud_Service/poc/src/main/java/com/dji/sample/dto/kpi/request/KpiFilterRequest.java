package com.dji.sample.dto.kpi.request;

import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
public class KpiFilterRequest {

    private UUID companyId;
    private UUID siteId;
    private UUID missionId;
    private String deviceSn;

    private OffsetDateTime fromDate;
    private OffsetDateTime toDate;
}