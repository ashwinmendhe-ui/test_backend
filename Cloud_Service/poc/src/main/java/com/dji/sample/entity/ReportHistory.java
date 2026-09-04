package com.dji.sample.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
    name = "report_history",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_report_history_device_playback",
            columnNames = {"device_sn", "playback_url"}
        )
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportHistory {

    @Id
    @Column(name = "history_id", nullable = false)
    private UUID historyId;

    @Column(name = "device_sn", nullable = false)
    private String deviceSn;

    @Column(name = "playback_url", nullable = false, columnDefinition = "TEXT")
    private String playbackUrl;
    
    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "site_id")
    private UUID siteId;

    @Column(name = "mission_id")
    private UUID missionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "start_time")
    private OffsetDateTime startTime;

    @Column(name = "end_time")
    private OffsetDateTime endTime;

    @Column(name = "total_time")
    private String totalTime;

    @Column(name = "total_recognition")
    private Integer totalRecognition;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "video_status", nullable = false)
    private String videoStatus;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "work_issue", columnDefinition = "TEXT")
    private String workIssue;

    @Column(name = "detection_types", columnDefinition = "TEXT")
    private String detectionTypes;

    @Column(name = "main_detection_type")
    private String mainDetectionType;

    @PrePersist
    public void prePersist() {
        if (historyId == null) {
            historyId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
        if (videoStatus == null || videoStatus.isBlank()) {
            videoStatus = "AVAILABLE";
        }
    }
}