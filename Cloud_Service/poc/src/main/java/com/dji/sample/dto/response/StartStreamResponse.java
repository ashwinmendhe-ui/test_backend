package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

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
}