package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class StreamInfoResponse {

    private UUID id;

    private String deviceSn;

    private UUID userId;

    private String sessionStatus;

    private String quality;

    private OffsetDateTime startedAt;

    private OffsetDateTime lastHeartbeatAt;

    private OffsetDateTime stoppedAt;

    // camelCase
    private String playbackUrl;
    private String mapUrl;

    // snake_case (reference FE compatibility)
    private String playback_url;
    private String map_url;

    private UUID missionId;

    private String videoId;

    // additional aliases
    private String url;
    private String streamUrl;
    private String liveUrl;
    private String cameraUrl;
}