package com.dji.sample.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "device_telemetry_history",
        indexes = {
                @Index(
                        name = "idx_device_telemetry_session_time",
                        columnList = "session_id, recorded_at"
                ),
                @Index(
                        name = "idx_device_telemetry_device_time",
                        columnList = "device_sn, recorded_at"
                )
        }
)
public class DeviceTelemetryHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "device_sn", nullable = false)
    private String deviceSn;

    @Column(name = "recorded_at", nullable = false)
    private OffsetDateTime recordedAt;

    @Column(name = "status")
    private String status;

    @Column(name = "battery")
    private Double battery;

    @Column(name = "network")
    private String network;

    @Column(name = "gps")
    private String gps;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    @Column(name = "altitude")
    private Double altitude;

    @Column(name = "speed")
    private Double speed;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (recordedAt == null) {
            recordedAt = OffsetDateTime.now();
        }

        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}