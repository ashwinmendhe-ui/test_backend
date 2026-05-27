package com.dji.sample.repository;

import com.dji.sample.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SiteRepository extends JpaRepository<Site, UUID> {

    List<Site> findByIsActiveTrueOrderByCreatedAtDesc();

    List<Site> findByCompanyIdAndIsActiveTrueOrderByCreatedAtDesc(UUID companyId);
    List<Site> findByCompanyId(UUID companyId);

    boolean existsByNameIgnoreCaseAndCompanyIdAndIsActiveTrue(
            String name,
            UUID companyId
    );
}