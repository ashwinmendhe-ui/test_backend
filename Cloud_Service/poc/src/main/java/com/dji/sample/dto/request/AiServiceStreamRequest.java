package com.dji.sample.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Request DTO for AI Service Stream Processing
 *
 * @author DHive Team
 * @date 2025-12-22
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiServiceStreamRequest {

    /**
     * RTMP URL that device is pushing to
     */
    @JsonProperty("uri")
    private String uri;

    /**
     * RTMP URL for vector map stream
     */
    @JsonProperty("vector_map_uri")
    private String vectorMapUri;

    /**
     * Stream identifier (device SN)
     */
    @JsonProperty("stream_id")
    private String streamId;

    @JsonProperty("emails")
    private List<String> emails;

    // ── Device Information ────────────────────────────────────────────

    @JsonProperty("device_id")
    private UUID deviceId;

    @JsonProperty("device_name")
    private String deviceName;

    // ── Work Information ──────────────────────────────────────────────

    @JsonProperty("company_id")
    private UUID companyId;

    @JsonProperty("company_name")
    private String companyName;

    @JsonProperty("site_id")
    private UUID siteId;

    @JsonProperty("site_name")
    private String siteName;

    @JsonProperty("mission_id")
    private UUID missionId;

    @JsonProperty("mission_name")
    private String missionName;

    @JsonProperty("user_id")
    private UUID userId;

    @JsonProperty("user_name")
    private String userName;

    @JsonProperty("session_start_time")
    private OffsetDateTime sessionStartTime;
}
