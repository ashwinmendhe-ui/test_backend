package com.dji.sample.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class StreamStatusResponse {

    private boolean active;

    private String deviceSn;

    private String sessionStatus;
}