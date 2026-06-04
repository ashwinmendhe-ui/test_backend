package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class StreamStatusResponse {

    private boolean active;

    private boolean streaming;

    private String deviceSn;

    private String sessionStatus;

    private UUID missionId;
}