package com.dji.sample.repository;

import com.dji.sample.entity.SubDevice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubDeviceRepository extends JpaRepository<SubDevice, Long> {

    Optional<SubDevice> findFirstByDeviceSnAndDeletedAtIsNullOrderByIdAsc(String deviceSn);
}