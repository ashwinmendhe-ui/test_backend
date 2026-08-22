package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
public class StartStreamResponse {

    private UUID sessionId;

    private UUID streamId;

    private UUID id;

    private String playbackUrl;

    private String sessionStatus;

    private String status;

    // reference FE compatibility
    private Integer viewerCount;

    private OffsetDateTime startTime;

    private Boolean canStop;

    private Boolean isSendHeartBeat;
    private Boolean joinedExisting;
}