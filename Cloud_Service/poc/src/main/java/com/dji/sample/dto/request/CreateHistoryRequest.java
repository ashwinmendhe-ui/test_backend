package com.dji.sample.dto.request;

import lombok.Data;

import java.util.UUID;

@Data
public class CreateHistoryRequest {
    private String deviceSn;
    private String playbackUrl;
    private UUID missionId;
    private UUID sessionId;
}