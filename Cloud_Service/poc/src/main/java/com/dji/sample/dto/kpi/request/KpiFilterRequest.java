package com.dji.sample.dto.kpi.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

@Getter
@Setter
@ToString
public class KpiFilterRequest {

    @Schema(description = "Filter by company ID")
    private UUID companyId;

    @Schema(description = "Filter by site ID")
    private UUID siteId;

    @Schema(description = "Filter by mission ID")
    private UUID missionId;

    @Schema(description = "Filter by device serial number", example = "go2-001")
    private String deviceSn;

   @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
@Schema(description = "Filter start date/time", example = "2026-07-01T00:00:00+05:30")
private OffsetDateTime fromDate;

@DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
@Schema(description = "Filter end date/time", example = "2026-07-07T23:59:59+05:30")
private OffsetDateTime toDate;
}