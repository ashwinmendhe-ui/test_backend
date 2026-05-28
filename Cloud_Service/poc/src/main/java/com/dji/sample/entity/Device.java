package com.dji.sample.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "devices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Device {

    @Id
    @Column(name = "device_id", nullable = false, updatable = false)
    private UUID deviceId;

    @Column(name = "device_name", nullable = false)
    private String deviceName;

    @Column(name = "device_type")
    private String deviceType;

    @Column(name = "brand_name")
    private String brandName;

    @Column(name = "model")
    private String model;

    @Column(name = "device_sn", unique = true)
    private String deviceSn;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "status")
    private String status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id")
    private Site site;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (deviceId == null) {
            deviceId = UUID.randomUUID();
        }

        if (status == null || status.isBlank()) {
            status = "INACTIVE";
        }

        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}