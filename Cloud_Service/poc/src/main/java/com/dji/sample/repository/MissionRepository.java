package com.dji.sample.repository;

import com.dji.sample.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, UUID> {

    List<Mission> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Mission> findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(UUID companyId);

    List<Mission> findBySiteIdAndIsActiveTrueOrderByCreatedAtDesc(UUID siteId);

    List<Mission> findBySiteIdAndIsActiveTrue(UUID siteId);

    List<Mission> findByCompanyIdAndIsActiveTrue(UUID companyId);
}