package com.dji.sample.repository;

import com.dji.sample.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceRepository extends JpaRepository<Device, Integer> {

    Optional<Device> findByDeviceIdAndDeletedAtIsNull(UUID deviceId);

    Optional<Device> findByDeviceSnAndDeletedAtIsNull(String deviceSn);

    boolean existsByDeviceSnAndDeletedAtIsNull(String deviceSn);

    List<Device> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Device> findBySite_SiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID siteId);

    List<Device> findByCompany_CompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID companyId);
    boolean existsByDeviceSn(String deviceSn);

}