package com.dji.sample.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "missions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "mission_id", nullable = false, unique = true, updatable = false)
    private UUID missionId;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "site_id")
    private UUID siteId;

    @Column(name = "location")
    private String location;

    @Column(name = "mission_name", nullable = false)
    private String missionName;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "mission_type")
    private String missionType;

    @Column(name = "file")
    private String file;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private OffsetDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    public void prePersist() {
        if (missionId == null) {
            missionId = UUID.randomUUID();
        }
    }
}