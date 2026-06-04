package com.dji.sample.repository;

import com.dji.sample.entity.Mission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MissionRepository extends JpaRepository<Mission, Integer> {

    List<Mission> findByDeletedAtIsNullOrderByCreatedAtDesc();

    List<Mission> findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID companyId);

    List<Mission> findBySiteIdAndDeletedAtIsNullOrderByCreatedAtDesc(UUID siteId);

    List<Mission> findBySiteIdAndDeletedAtIsNull(UUID siteId);

    List<Mission> findByCompanyIdAndDeletedAtIsNull(UUID companyId);

    Optional<Mission> findByMissionIdAndDeletedAtIsNull(UUID missionId);
} 