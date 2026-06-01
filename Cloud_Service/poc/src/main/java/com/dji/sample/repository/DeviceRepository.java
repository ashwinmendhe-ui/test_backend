package com.dji.sample.repository;

import com.dji.sample.entity.Device;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;
import java.util.Optional;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    List<Device> findBySite_SiteId(UUID siteId);

    List<Device> findByCompany_CompanyId(UUID companyId);

    boolean existsByDeviceSn(String deviceSn);
    Optional<Device> findByDeviceSn(String deviceSn);
}