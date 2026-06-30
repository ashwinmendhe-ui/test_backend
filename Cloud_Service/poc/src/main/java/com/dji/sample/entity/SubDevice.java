package com.dji.sample.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "sub_devices")
@Getter
@Setter
public class SubDevice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "sub_device_id")
    private UUID subDeviceId;

    @Column(name = "device_sn")
    private String deviceSn;

    @Column(name = "sn")
    private String sn;

    @Column(name = "domain")
    private String domain;

    @Column(name = "type")
    private Integer type;

    @Column(name = "sub_type")
    private Integer subType;

    @Column(name = "\"index\"")
    private String index;

    @Column(name = "thing_version")
    private String thingVersion;

    @Column(name = "model")
    private String model;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;
}