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
@Table(name = "livestream_sessions")
public class LiveStreamSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "device_sn", nullable = false)
    private String deviceSn;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "session_status")
    private String sessionStatus;

    @Column(name = "quality")
    private String quality;

    @Column(name = "started_at", nullable = false)
    private OffsetDateTime startedAt;

    @Column(name = "last_heartbeat_at", nullable = false)
    private OffsetDateTime lastHeartbeatAt;

    @Column(name = "stopped_at")
    private OffsetDateTime stoppedAt;

    @Column(name = "playback_url", columnDefinition = "TEXT")
    private String playbackUrl;

    @Column(name = "mission_id")
    private UUID missionId;

    @Column(name = "video_id")
    private String videoId;
}