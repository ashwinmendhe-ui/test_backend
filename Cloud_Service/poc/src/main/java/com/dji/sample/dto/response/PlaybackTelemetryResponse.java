package com.dji.sample.dto.response;

import lombok.*;

import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlaybackTelemetryResponse {

    private OffsetDateTime recordedAt;
    private Long offsetMs;

    private String status;
    private Double battery;
    private String network;
    private String gps;

    private Double latitude;
    private Double longitude;
    private Double altitude;
    private Double speed;
}